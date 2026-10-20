package com.shorashim.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MessageType {
    @SerialName("TEXT")
    TEXT,
    @SerialName("INVITATION")
    INVITATION,
}

/** Row of `user_key_backups`: a chat private key encrypted with a key derived from the user's password. */
@Serializable
data class ChatKeyBackupDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("key_fingerprint") val keyFingerprint: String = "",
    @SerialName("public_key") val publicKey: String = "",
    @SerialName("wrapped_private_key") val wrappedPrivateKey: String = "",
    val salt: String = "",
    val iv: String = "",
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int = 150000,
)

@Serializable
data class UserPublicKeyDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("public_key") val publicKey: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ConversationDto(
    val id: String = "",
    @SerialName("participant1_id") val participant1Id: String = "",
    @SerialName("participant2_id") val participant2Id: String = "",
    @SerialName("last_message_at") val lastMessageAt: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class Conversation(
    val id: String,
    val otherUserId: String,
    val otherUserName: String = "",
    val otherUserUsername: String? = null,
    val otherUserAvatar: String? = null,
    val isOtherUserOnline: Boolean = false,
    val lastMessageText: String? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
)

@Serializable
data class ChatMessageDto(
    val id: String = "",
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("recipient_id") val recipientId: String = "",
    @SerialName("message_type") val messageType: MessageType = MessageType.TEXT,
    val ciphertext: String = "",
    val iv: String = "",
    @SerialName("encrypted_key_sender") val encryptedKeySender: String = "",
    @SerialName("encrypted_key_recipient") val encryptedKeyRecipient: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class DecryptedChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val messageType: MessageType,
    val plaintext: String,
    val isRead: Boolean,
    val createdAt: String?,
    val isFromMe: Boolean,
    val invitationPayload: InvitationMessagePayload? = null,
    /** False when no key on this device can open the message (it was encrypted for a key that is gone). */
    val isDecryptable: Boolean = true,
)

@Serializable
data class InvitationMessagePayload(
    val url: String,
    val familyName: String? = null,
    val token: String? = null,
)

@Serializable
data class MutualFamilyConnection(
    @SerialName("family_id") val familyId: String = "",
    @SerialName("family_name") val familyName: String = "",
    @SerialName("person_id") val personId: String = "",
    @SerialName("person_name") val personName: String = "",
    val role: String = "",
)
