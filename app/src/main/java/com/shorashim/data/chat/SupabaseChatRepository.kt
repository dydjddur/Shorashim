package com.shorashim.data.chat

import android.content.Context
import android.util.Log
import com.shorashim.core.common.InvitationUrlValidator
import com.shorashim.core.crypto.ChatCryptoManager
import com.shorashim.core.model.ChatMessageDto
import com.shorashim.core.model.Conversation
import com.shorashim.core.model.ConversationDto
import com.shorashim.core.model.DecryptedChatMessage
import com.shorashim.core.model.InvitationMessagePayload
import com.shorashim.core.model.MessageType
import com.shorashim.core.model.UserProfileDto
import com.shorashim.core.model.UserPublicKeyDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * End-to-end encrypted 1:1 chat on top of Supabase.
 *
 * Everything here is **server-backed and honest**: a message only counts as sent when the insert reached the
 * database, a peer without a published public key cannot be messaged, and there are no local "demo" conversations.
 * (An earlier version silently fell back to SharedPreferences and even invented a key pair for a peer it could not
 * find, which made messages look delivered while the other person could never read them.)
 */
class SupabaseChatRepository(
    private val supabase: SupabaseClient?,
    private val cryptoManager: ChatCryptoManager = ChatCryptoManager(),
    @Suppress("unused") private val context: Context? = null,
) : ChatRepository {

    companion object {
        private const val TAG = "SupabaseChatRepo"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
        private val UUID_REGEX =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        const val ERR_PEER_NO_KEY = "המשתמש עדיין לא הפעיל הצפנה. הוא יוכל לקבל הודעות מוצפנות אחרי שייכנס לאפליקציה."
        const val ERR_NOT_CONFIGURED = "מערכת הצ'אט עדיין לא הוגדרה בשרת (חסרות טבלאות Supabase). יש להריץ את קובץ ההגדרה supabase/APPLY_PENDING.sql."
    }

    private fun requireClient(): SupabaseClient = supabase ?: error("Supabase לא מוגדר")

    private fun currentUserId(client: SupabaseClient): String =
        client.auth.currentUserOrNull()?.id ?: error("המשתמש אינו מחובר")

    private fun isUuid(str: String?): Boolean = !str.isNullOrBlank() && UUID_REGEX.matches(str.trim())

    /** Turns PostgREST's "table not found" into a message the user (and a developer) can act on. */
    private fun friendly(t: Throwable): Throwable {
        val m = t.message.orEmpty()
        return if (m.contains("PGRST205") || m.contains("schema cache", ignoreCase = true) ||
            (m.contains("does not exist", ignoreCase = true) && m.contains("relation", ignoreCase = true))
        ) {
            IllegalStateException(ERR_NOT_CONFIGURED, t)
        } else {
            t
        }
    }

    override suspend fun ensurePublicKeyPublished(): Result<Unit> = runCatching {
        val client = requireClient()
        val me = currentUserId(client)
        val pubKey = cryptoManager.getPublicKeyBase64(me)
        client.from("user_public_keys").upsert(
            buildJsonObject {
                put("user_id", me)
                put("public_key", pubKey)
            },
        )
        Unit
    }.mapFailure(::friendly).onFailure { Log.w(TAG, "Could not publish E2EE public key: ${it.message}") }

    override suspend fun getPeerPublicKey(userId: String): Result<String?> = runCatching {
        val client = requireClient()
        client.from("user_public_keys")
            .select { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<UserPublicKeyDto>()
            ?.publicKey
            ?.takeIf { it.isNotBlank() }
    }.mapFailure(::friendly)

    override suspend fun getConversations(): Result<List<Conversation>> = runCatching {
        val client = requireClient()
        val me = currentUserId(client)

        val convDtos = client.from("conversations").select {
            filter {
                or {
                    eq("participant1_id", me)
                    eq("participant2_id", me)
                }
            }
        }.decodeList<ConversationDto>()
        if (convDtos.isEmpty()) return@runCatching emptyList()

        val otherIds = convDtos.map { if (it.participant1Id == me) it.participant2Id else it.participant1Id }.distinct()
        val profiles = client.from("profiles")
            .select { filter { isIn("id", otherIds) } }
            .decodeList<UserProfileDto>()
            .associateBy { it.id }

        // One query for all unread messages addressed to me.
        val unread = runCatching {
            client.from("chat_messages").select {
                filter {
                    eq("recipient_id", me)
                    eq("is_read", false)
                }
            }.decodeList<ChatMessageDto>().groupingBy { it.conversationId }.eachCount()
        }.getOrDefault(emptyMap())

        convDtos.map { conv ->
            val otherId = if (conv.participant1Id == me) conv.participant2Id else conv.participant1Id
            val profile = profiles[otherId]
            val name = profile?.displayName?.ifBlank { null }
                ?: profile?.fullName?.ifBlank { null }
                ?: profile?.username
                ?: "משתמש"

            val last = runCatching {
                client.from("chat_messages").select {
                    filter { eq("conversation_id", conv.id) }
                    order("created_at", Order.DESCENDING)
                    limit(1)
                }.decodeList<ChatMessageDto>().firstOrNull()
            }.getOrNull()

            val preview = last?.let { dto ->
                if (dto.messageType == MessageType.INVITATION) {
                    "🌲 הזמנה לעץ משפחה"
                } else {
                    decrypt(me, dto).getOrNull() ?: "🔒 הודעה מוצפנת"
                }
            }

            Conversation(
                id = conv.id,
                otherUserId = otherId,
                otherUserName = name,
                otherUserUsername = profile?.username,
                otherUserAvatar = profile?.avatarUrl,
                lastMessageText = preview,
                lastMessageAt = last?.createdAt ?: conv.lastMessageAt,
                unreadCount = unread[conv.id] ?: 0,
            )
        }.sortedByDescending { it.lastMessageAt.orEmpty() }
    }.mapFailure(::friendly).onFailure { Log.w(TAG, "getConversations failed: ${it.message}") }

    override suspend fun getOrCreateConversation(otherUserId: String): Result<String> = runCatching {
        val client = requireClient()
        val me = currentUserId(client)
        require(isUuid(otherUserId)) { "מזהה משתמש לא תקין" }

        // The table enforces participant1_id < participant2_id.
        val p1 = if (me < otherUserId) me else otherUserId
        val p2 = if (me < otherUserId) otherUserId else me

        suspend fun find(): ConversationDto? = client.from("conversations").select {
            filter {
                eq("participant1_id", p1)
                eq("participant2_id", p2)
            }
        }.decodeSingleOrNull<ConversationDto>()

        find()?.let { return@runCatching it.id }

        try {
            client.from("conversations").insert(
                buildJsonObject {
                    put("participant1_id", p1)
                    put("participant2_id", p2)
                },
            ) { select() }.decodeSingle<ConversationDto>().id
        } catch (e: Throwable) {
            // The other side may have created it a moment ago (unique constraint): use theirs.
            find()?.id ?: throw e
        }
    }.mapFailure(::friendly)

    override suspend fun getMessages(conversationId: String): Result<List<DecryptedChatMessage>> = runCatching {
        val client = requireClient()
        val me = currentUserId(client)

        val dtos = client.from("chat_messages").select {
            filter { eq("conversation_id", conversationId) }
            order("created_at", Order.ASCENDING)
        }.decodeList<ChatMessageDto>()

        dtos.map { dto ->
            val isSender = dto.senderId == me
            val plaintext = decrypt(me, dto).getOrElse { "🔒 תוכן מוצפן (לא ניתן לפענוח במכשיר זה)" }
            toDecrypted(dto, plaintext, isSender)
        }
    }.mapFailure(::friendly).onFailure { Log.w(TAG, "getMessages failed: ${it.message}") }

    override suspend fun sendTextMessage(
        conversationId: String,
        recipientUserId: String,
        text: String,
    ): Result<DecryptedChatMessage> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("ההודעה ריקה"))

        // A pasted invitation link becomes a proper invitation card.
        if (InvitationUrlValidator.isValidInvitationUrl(trimmed)) {
            return sendInvitationMessage(
                conversationId = conversationId,
                recipientUserId = recipientUserId,
                invitationUrl = trimmed,
                familyName = null,
                token = InvitationUrlValidator.extractToken(trimmed),
            )
        }
        return sendEncrypted(conversationId, recipientUserId, MessageType.TEXT, trimmed, null)
    }

    override suspend fun sendInvitationMessage(
        conversationId: String,
        recipientUserId: String,
        invitationUrl: String,
        familyName: String?,
        token: String?,
    ): Result<DecryptedChatMessage> {
        if (!InvitationUrlValidator.isValidInvitationUrl(invitationUrl)) {
            return Result.failure(IllegalArgumentException("כתובת ההזמנה אינה תקינה"))
        }
        val payload = InvitationMessagePayload(
            url = invitationUrl.trim(),
            familyName = familyName,
            token = token ?: InvitationUrlValidator.extractToken(invitationUrl),
        )
        return sendEncrypted(
            conversationId,
            recipientUserId,
            MessageType.INVITATION,
            json.encodeToString(InvitationMessagePayload.serializer(), payload),
            payload,
        )
    }

    private suspend fun sendEncrypted(
        conversationId: String,
        recipientUserId: String,
        type: MessageType,
        plaintext: String,
        invitation: InvitationMessagePayload?,
    ): Result<DecryptedChatMessage> = runCatching {
        val client = requireClient()
        val me = currentUserId(client)

        // Make sure the peer can decrypt: they must have published a key. Never invent one.
        val peerKey = getPeerPublicKey(recipientUserId).getOrThrow() ?: error(ERR_PEER_NO_KEY)
        ensurePublicKeyPublished().getOrThrow()

        val enc = cryptoManager.encryptMessage(
            senderUserId = me,
            recipientPublicKeyBase64 = peerKey,
            plaintext = plaintext,
        )

        val inserted = client.from("chat_messages").insert(
            buildJsonObject {
                put("conversation_id", conversationId)
                put("sender_id", me)
                put("recipient_id", recipientUserId)
                put("message_type", if (type == MessageType.INVITATION) "INVITATION" else "TEXT")
                put("ciphertext", enc.ciphertextBase64)
                put("iv", enc.ivBase64)
                put("encrypted_key_sender", enc.encryptedKeySenderBase64)
                put("encrypted_key_recipient", enc.encryptedKeyRecipientBase64)
            },
        ) { select() }.decodeSingle<ChatMessageDto>()

        // Best effort: bump the conversation so it sorts first for both people.
        runCatching {
            client.from("conversations").update(
                buildJsonObject { put("last_message_at", inserted.createdAt ?: Instant.now().toString()) },
            ) { filter { eq("id", conversationId) } }
        }

        toDecrypted(inserted, plaintext, isFromMe = true, invitationOverride = invitation)
    }.mapFailure(::friendly).onFailure { Log.w(TAG, "send failed: ${it.message}") }

    override suspend fun markMessagesAsRead(conversationId: String): Result<Unit> = runCatching {
        val client = requireClient()
        val me = currentUserId(client)
        client.from("chat_messages").update(
            buildJsonObject { put("is_read", true) },
        ) {
            filter {
                eq("conversation_id", conversationId)
                eq("recipient_id", me)
                eq("is_read", false)
            }
        }
        Unit
    }

    // ---------------------------------------------------------------------------------------------

    private fun decrypt(me: String, dto: ChatMessageDto): Result<String> = cryptoManager.decryptMessage(
        currentUserId = me,
        isSender = dto.senderId == me,
        encryptedKeySenderBase64 = dto.encryptedKeySender,
        encryptedKeyRecipientBase64 = dto.encryptedKeyRecipient,
        ivBase64 = dto.iv,
        ciphertextBase64 = dto.ciphertext,
    )

    private fun toDecrypted(
        dto: ChatMessageDto,
        plaintext: String,
        isFromMe: Boolean,
        invitationOverride: InvitationMessagePayload? = null,
    ): DecryptedChatMessage {
        var invitation = invitationOverride
        if (invitation == null && (dto.messageType == MessageType.INVITATION || InvitationUrlValidator.isValidInvitationUrl(plaintext))) {
            invitation = runCatching { json.decodeFromString<InvitationMessagePayload>(plaintext) }.getOrNull()
                ?: if (InvitationUrlValidator.isValidInvitationUrl(plaintext)) {
                    InvitationMessagePayload(
                        url = plaintext.trim(),
                        token = InvitationUrlValidator.extractToken(plaintext),
                    )
                } else null
        }
        return DecryptedChatMessage(
            id = dto.id,
            conversationId = dto.conversationId,
            senderId = dto.senderId,
            recipientId = dto.recipientId,
            messageType = dto.messageType,
            plaintext = plaintext,
            isRead = dto.isRead,
            createdAt = dto.createdAt,
            isFromMe = isFromMe,
            invitationPayload = invitation,
        )
    }
}

private inline fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> =
    fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(transform(it)) })
