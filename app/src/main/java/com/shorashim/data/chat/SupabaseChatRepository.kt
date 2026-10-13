package com.shorashim.data.chat

import android.content.Context
import android.content.SharedPreferences
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

class SupabaseChatRepository(
    private val supabase: SupabaseClient?,
    private val cryptoManager: ChatCryptoManager = ChatCryptoManager(),
    private val context: Context? = null,
) : ChatRepository {

    companion object {
        private const val TAG = "SupabaseChatRepo"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
        private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }

    private val prefs: SharedPreferences? =
        context?.getSharedPreferences("shorashim_chat_local_prefs", Context.MODE_PRIVATE)

    private fun isUuid(str: String?): Boolean = !str.isNullOrBlank() && UUID_REGEX.matches(str.trim())

    private fun getCurrentUserId(): String = supabase?.auth?.currentUserOrNull()?.id ?: "local_user"

    override suspend fun ensurePublicKeyPublished(): Result<Unit> {
        val userId = getCurrentUserId()
        return runCatching {
            val pubKey = cryptoManager.getPublicKeyBase64(userId)
            val client = supabase
            if (client != null && isUuid(userId)) {
                try {
                    val dto = UserPublicKeyDto(
                        userId = userId,
                        publicKey = pubKey,
                    )
                    client.from("user_public_keys").upsert(dto)
                } catch (e: Throwable) {
                    Log.w(TAG, "Could not push public key to remote Supabase, saved locally: ${e.message}")
                }
            }
            saveLocalPublicKey(userId, pubKey)
            Unit
        }.onFailure {
            Log.e(TAG, "Failed to publish E2EE public key", it)
        }
    }

    override suspend fun getPeerPublicKey(userId: String): Result<String?> {
        // 1. Try remote if UUID
        val client = supabase
        if (client != null && isUuid(userId)) {
            val remoteKey = runCatching {
                val row = client.from("user_public_keys").select {
                    filter { eq("user_id", userId) }
                }.decodeSingleOrNull<UserPublicKeyDto>()
                row?.publicKey
            }.getOrNull()

            if (!remoteKey.isNullOrBlank()) {
                saveLocalPublicKey(userId, remoteKey)
                return Result.success(remoteKey)
            }
        }

        // 2. Check local saved key
        val localKey = getLocalPublicKey(userId)
        if (!localKey.isNullOrBlank()) {
            return Result.success(localKey)
        }

        // 3. Fallback / demo friend key generation
        return runCatching {
            val generatedKey = cryptoManager.getPublicKeyBase64(userId)
            saveLocalPublicKey(userId, generatedKey)
            generatedKey
        }
    }

    override suspend fun getConversations(): Result<List<Conversation>> {
        val currentUserId = getCurrentUserId()
        val client = supabase

        return runCatching {
            val remoteConversations = mutableListOf<Conversation>()

            if (client != null && isUuid(currentUserId)) {
                try {
                    val convDtos = client.from("conversations").select {
                        filter {
                            or {
                                eq("participant1_id", currentUserId)
                                eq("participant2_id", currentUserId)
                            }
                        }
                    }.decodeList<ConversationDto>()

                    val otherUserIds = convDtos.map { dto ->
                        if (dto.participant1Id == currentUserId) dto.participant2Id else dto.participant1Id
                    }.distinct()

                    val profilesMap = if (otherUserIds.isNotEmpty()) {
                        client.from("profiles").select {
                            filter { isIn("id", otherUserIds) }
                        }.decodeList<UserProfileDto>().associateBy { it.id }
                    } else emptyMap()

                    for (convDto in convDtos) {
                        val otherId = if (convDto.participant1Id == currentUserId) convDto.participant2Id else convDto.participant1Id
                        val profile = profilesMap[otherId]
                        val name = profile?.displayName?.ifBlank { profile.fullName }?.ifBlank { null }
                            ?: profile?.fullName?.ifBlank { null }
                            ?: "משתמש"

                        val lastMsgDto = runCatching {
                            client.from("chat_messages").select {
                                filter { eq("conversation_id", convDto.id) }
                                limit(1)
                            }.decodeList<ChatMessageDto>().firstOrNull()
                        }.getOrNull()

                        val lastMsgText = if (lastMsgDto != null) {
                            val decrypted = cryptoManager.decryptMessage(
                                currentUserId = currentUserId,
                                isSender = lastMsgDto.senderId == currentUserId,
                                encryptedKeySenderBase64 = lastMsgDto.encryptedKeySender,
                                encryptedKeyRecipientBase64 = lastMsgDto.encryptedKeyRecipient,
                                ivBase64 = lastMsgDto.iv,
                                ciphertextBase64 = lastMsgDto.ciphertext,
                            ).getOrNull()

                            if (lastMsgDto.messageType == MessageType.INVITATION) {
                                "🌲 הזמנה לעץ משפחה"
                            } else {
                                decrypted ?: "🔒 הודעה מוצפנת"
                            }
                        } else null

                        remoteConversations.add(
                            Conversation(
                                id = convDto.id,
                                otherUserId = otherId,
                                otherUserName = name,
                                otherUserUsername = profile?.username,
                                otherUserAvatar = profile?.avatarUrl,
                                lastMessageText = lastMsgText,
                                lastMessageAt = convDto.lastMessageAt,
                            )
                        )
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Remote conversations query failed, falling back to local: ${e.message}")
                }
            }

            // Merge with local conversations
            val localConversations = getLocalConversations()
            val mergedMap = mutableMapOf<String, Conversation>()
            for (c in remoteConversations) {
                mergedMap[c.otherUserId] = c
            }
            for (c in localConversations) {
                if (!mergedMap.containsKey(c.otherUserId)) {
                    mergedMap[c.otherUserId] = c
                }
            }

            mergedMap.values.sortedByDescending { it.lastMessageAt ?: "" }
        }
    }

    override suspend fun getOrCreateConversation(otherUserId: String): Result<String> {
        val currentUserId = getCurrentUserId()
        val client = supabase

        // 1. Try remote if both are valid UUIDs
        if (client != null && isUuid(currentUserId) && isUuid(otherUserId)) {
            val remoteResult = runCatching {
                val p1 = if (currentUserId < otherUserId) currentUserId else otherUserId
                val p2 = if (currentUserId < otherUserId) otherUserId else currentUserId

                val existing = client.from("conversations").select {
                    filter {
                        eq("participant1_id", p1)
                        eq("participant2_id", p2)
                    }
                }.decodeSingleOrNull<ConversationDto>()

                if (existing != null) {
                    return@runCatching existing.id
                }

                val insertJson = buildJsonObject {
                    put("participant1_id", p1)
                    put("participant2_id", p2)
                }
                val created = client.from("conversations").insert(insertJson) {
                    select()
                }.decodeSingle<ConversationDto>()

                created.id
            }

            if (remoteResult.isSuccess) {
                return remoteResult
            } else {
                Log.w(TAG, "Remote getOrCreateConversation failed, falling back to local: ${remoteResult.exceptionOrNull()?.message}")
            }
        }

        // 2. Local conversation fallback
        return runCatching {
            val p1 = if (currentUserId < otherUserId) currentUserId else otherUserId
            val p2 = if (currentUserId < otherUserId) otherUserId else currentUserId
            val localConvId = "conv_${Math.abs(p1.hashCode())}_${Math.abs(p2.hashCode())}"
            upsertLocalConversation(localConvId, otherUserId)
            localConvId
        }
    }

    override suspend fun getMessages(conversationId: String): Result<List<DecryptedChatMessage>> {
        val currentUserId = getCurrentUserId()
        val client = supabase

        return runCatching {
            var dtos: List<ChatMessageDto>? = null

            if (client != null && isUuid(conversationId)) {
                try {
                    dtos = client.from("chat_messages").select {
                        filter { eq("conversation_id", conversationId) }
                    }.decodeList<ChatMessageDto>()
                } catch (e: Throwable) {
                    Log.w(TAG, "Remote getMessages failed: ${e.message}")
                }
            }

            if (dtos == null || dtos.isEmpty()) {
                dtos = getLocalMessages(conversationId)
            }

            dtos.map { dto ->
                val isSender = dto.senderId == currentUserId
                val decryptedTextResult = cryptoManager.decryptMessage(
                    currentUserId = currentUserId,
                    isSender = isSender,
                    encryptedKeySenderBase64 = dto.encryptedKeySender,
                    encryptedKeyRecipientBase64 = dto.encryptedKeyRecipient,
                    ivBase64 = dto.iv,
                    ciphertextBase64 = dto.ciphertext,
                )

                val plaintext = decryptedTextResult.getOrElse {
                    "🔒 תוכן מוצפן (לא ניתן לפענוח במכשיר זה)"
                }

                var invitation: InvitationMessagePayload? = null
                if (dto.messageType == MessageType.INVITATION || InvitationUrlValidator.isValidInvitationUrl(plaintext)) {
                    invitation = try {
                        json.decodeFromString<InvitationMessagePayload>(plaintext)
                    } catch (e: Throwable) {
                        if (InvitationUrlValidator.isValidInvitationUrl(plaintext)) {
                            InvitationMessagePayload(
                                url = plaintext.trim(),
                                token = InvitationUrlValidator.extractToken(plaintext),
                            )
                        } else null
                    }
                }

                DecryptedChatMessage(
                    id = dto.id,
                    conversationId = dto.conversationId,
                    senderId = dto.senderId,
                    recipientId = dto.recipientId,
                    messageType = dto.messageType,
                    plaintext = plaintext,
                    isRead = dto.isRead,
                    createdAt = dto.createdAt,
                    isFromMe = isSender,
                    invitationPayload = invitation,
                )
            }.sortedBy { it.createdAt }
        }
    }

    override suspend fun sendTextMessage(
        conversationId: String,
        recipientUserId: String,
        text: String,
    ): Result<DecryptedChatMessage> {
        val currentUserId = getCurrentUserId()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty"))

        // Check if text is an invitation link
        if (InvitationUrlValidator.isValidInvitationUrl(trimmed)) {
            val token = InvitationUrlValidator.extractToken(trimmed)
            return sendInvitationMessage(
                conversationId = conversationId,
                recipientUserId = recipientUserId,
                invitationUrl = trimmed,
                familyName = null,
                token = token,
            )
        }

        return runCatching {
            val peerPubKey = getPeerPublicKey(recipientUserId).getOrThrow()
                ?: return Result.failure(IllegalStateException("המשתמש טרם הגדיר מפתח הצפנה במכשירו."))

            ensurePublicKeyPublished()

            // E2EE Encrypt
            val encrypted = cryptoManager.encryptMessage(
                senderUserId = currentUserId,
                recipientPublicKeyBase64 = peerPubKey,
                plaintext = trimmed,
            )

            val msgId = UUID.randomUUID().toString()
            val nowStr = Instant.now().toString()
            val client = supabase

            var remoteSuccess = false
            if (client != null && isUuid(conversationId) && isUuid(currentUserId) && isUuid(recipientUserId)) {
                try {
                    val insertPayload = buildJsonObject {
                        put("conversation_id", conversationId)
                        put("sender_id", currentUserId)
                        put("recipient_id", recipientUserId)
                        put("message_type", "TEXT")
                        put("ciphertext", encrypted.ciphertextBase64)
                        put("iv", encrypted.ivBase64)
                        put("encrypted_key_sender", encrypted.encryptedKeySenderBase64)
                        put("encrypted_key_recipient", encrypted.encryptedKeyRecipientBase64)
                    }

                    client.from("chat_messages").insert(insertPayload)
                    client.from("conversations").update(
                        buildJsonObject { put("last_message_at", nowStr) }
                    ) {
                        filter { eq("id", conversationId) }
                    }
                    remoteSuccess = true
                } catch (e: Throwable) {
                    Log.w(TAG, "Remote sendTextMessage failed, storing locally: ${e.message}")
                }
            }

            val chatMessageDto = ChatMessageDto(
                id = msgId,
                conversationId = conversationId,
                senderId = currentUserId,
                recipientId = recipientUserId,
                messageType = MessageType.TEXT,
                ciphertext = encrypted.ciphertextBase64,
                iv = encrypted.ivBase64,
                encryptedKeySender = encrypted.encryptedKeySenderBase64,
                encryptedKeyRecipient = encrypted.encryptedKeyRecipientBase64,
                isRead = false,
                createdAt = nowStr,
            )

            // Save locally for offline and fallback support
            saveLocalMessage(chatMessageDto)
            updateLocalConversationPreview(conversationId, recipientUserId, trimmed, nowStr)

            DecryptedChatMessage(
                id = msgId,
                conversationId = conversationId,
                senderId = currentUserId,
                recipientId = recipientUserId,
                messageType = MessageType.TEXT,
                plaintext = trimmed,
                isRead = false,
                createdAt = nowStr,
                isFromMe = true,
            )
        }.onFailure {
            Log.e(TAG, "Failed to send text message", it)
        }
    }

    override suspend fun sendInvitationMessage(
        conversationId: String,
        recipientUserId: String,
        invitationUrl: String,
        familyName: String?,
        token: String?,
    ): Result<DecryptedChatMessage> {
        val currentUserId = getCurrentUserId()

        if (!InvitationUrlValidator.isValidInvitationUrl(invitationUrl)) {
            return Result.failure(IllegalArgumentException("כתובת ההזמנה אינה תקינה לפי הפרוטוקול המאובטח"))
        }

        return runCatching {
            val peerPubKey = getPeerPublicKey(recipientUserId).getOrThrow()
                ?: return Result.failure(IllegalStateException("המשתמש טרם הגדיר מפתח הצפנה במכשירו."))

            ensurePublicKeyPublished()

            val payloadObj = InvitationMessagePayload(
                url = invitationUrl.trim(),
                familyName = familyName,
                token = token ?: InvitationUrlValidator.extractToken(invitationUrl),
            )
            val payloadString = json.encodeToString(InvitationMessagePayload.serializer(), payloadObj)

            // E2EE Encrypt
            val encrypted = cryptoManager.encryptMessage(
                senderUserId = currentUserId,
                recipientPublicKeyBase64 = peerPubKey,
                plaintext = payloadString,
            )

            val msgId = UUID.randomUUID().toString()
            val nowStr = Instant.now().toString()
            val client = supabase

            if (client != null && isUuid(conversationId) && isUuid(currentUserId) && isUuid(recipientUserId)) {
                try {
                    val insertPayload = buildJsonObject {
                        put("conversation_id", conversationId)
                        put("sender_id", currentUserId)
                        put("recipient_id", recipientUserId)
                        put("message_type", "INVITATION")
                        put("ciphertext", encrypted.ciphertextBase64)
                        put("iv", encrypted.ivBase64)
                        put("encrypted_key_sender", encrypted.encryptedKeySenderBase64)
                        put("encrypted_key_recipient", encrypted.encryptedKeyRecipientBase64)
                    }

                    client.from("chat_messages").insert(insertPayload)
                    client.from("conversations").update(
                        buildJsonObject { put("last_message_at", nowStr) }
                    ) {
                        filter { eq("id", conversationId) }
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Remote sendInvitationMessage failed, storing locally: ${e.message}")
                }
            }

            val chatMessageDto = ChatMessageDto(
                id = msgId,
                conversationId = conversationId,
                senderId = currentUserId,
                recipientId = recipientUserId,
                messageType = MessageType.INVITATION,
                ciphertext = encrypted.ciphertextBase64,
                iv = encrypted.ivBase64,
                encryptedKeySender = encrypted.encryptedKeySenderBase64,
                encryptedKeyRecipient = encrypted.encryptedKeyRecipientBase64,
                isRead = false,
                createdAt = nowStr,
            )

            saveLocalMessage(chatMessageDto)
            updateLocalConversationPreview(conversationId, recipientUserId, "🌲 הזמנה לעץ משפחה", nowStr)

            DecryptedChatMessage(
                id = msgId,
                conversationId = conversationId,
                senderId = currentUserId,
                recipientId = recipientUserId,
                messageType = MessageType.INVITATION,
                plaintext = payloadString,
                isRead = false,
                createdAt = nowStr,
                isFromMe = true,
                invitationPayload = payloadObj,
            )
        }.onFailure {
            Log.e(TAG, "Failed to send invitation message", it)
        }
    }

    override suspend fun markMessagesAsRead(conversationId: String): Result<Unit> {
        val currentUserId = getCurrentUserId()
        val client = supabase

        return runCatching {
            if (client != null && isUuid(conversationId) && isUuid(currentUserId)) {
                try {
                    client.from("chat_messages").update(
                        buildJsonObject { put("is_read", true) }
                    ) {
                        filter {
                            eq("conversation_id", conversationId)
                            eq("recipient_id", currentUserId)
                            eq("is_read", false)
                        }
                    }
                } catch (_: Throwable) {}
            }
            Unit
        }
    }

    // --- Local Persistence Helpers ---

    private fun saveLocalPublicKey(userId: String, key: String) {
        prefs?.edit()?.putString("pubkey_$userId", key)?.apply()
    }

    private fun getLocalPublicKey(userId: String): String? {
        return prefs?.getString("pubkey_$userId", null)
    }

    private fun getLocalConversations(): List<Conversation> {
        val raw = prefs?.getString("conversations_list", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Conversation>>(raw)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun upsertLocalConversation(convId: String, otherUserId: String) {
        val currentList = getLocalConversations().toMutableList()
        if (currentList.none { it.id == convId }) {
            val displayName = when (otherUserId) {
                "friend_default_1" -> "אבי לוי"
                "friend_default_2" -> "שרה ישראלי"
                else -> "משתמש"
            }
            val username = when (otherUserId) {
                "friend_default_1" -> "avi_levi"
                "friend_default_2" -> "sara_israeli"
                else -> null
            }
            currentList.add(
                Conversation(
                    id = convId,
                    otherUserId = otherUserId,
                    otherUserName = displayName,
                    otherUserUsername = username,
                    lastMessageAt = Instant.now().toString(),
                )
            )
            prefs?.edit()?.putString("conversations_list", json.encodeToString(currentList))?.apply()
        }
    }

    private fun updateLocalConversationPreview(
        convId: String,
        otherUserId: String,
        lastText: String,
        time: String,
    ) {
        val currentList = getLocalConversations().toMutableList()
        val idx = currentList.indexOfFirst { it.id == convId }
        val displayName = when (otherUserId) {
            "friend_default_1" -> "אבי לוי"
            "friend_default_2" -> "שרה ישראלי"
            else -> "משתמש"
        }
        val username = when (otherUserId) {
            "friend_default_1" -> "avi_levi"
            "friend_default_2" -> "sara_israeli"
            else -> null
        }
        val updated = Conversation(
            id = convId,
            otherUserId = otherUserId,
            otherUserName = displayName,
            otherUserUsername = username,
            lastMessageText = lastText,
            lastMessageAt = time,
        )
        if (idx >= 0) {
            currentList[idx] = updated
        } else {
            currentList.add(updated)
        }
        prefs?.edit()?.putString("conversations_list", json.encodeToString(currentList))?.apply()
    }

    private fun getLocalMessages(conversationId: String): List<ChatMessageDto> {
        val raw = prefs?.getString("messages_$conversationId", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<ChatMessageDto>>(raw)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun saveLocalMessage(msg: ChatMessageDto) {
        val current = getLocalMessages(msg.conversationId).toMutableList()
        current.add(msg)
        prefs?.edit()?.putString("messages_${msg.conversationId}", json.encodeToString(current))?.apply()
    }
}

