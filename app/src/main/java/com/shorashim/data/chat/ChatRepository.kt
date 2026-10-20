package com.shorashim.data.chat

import com.shorashim.core.model.Conversation
import com.shorashim.core.model.DecryptedChatMessage
import kotlinx.coroutines.flow.Flow

/** A received message plus the sender's display name, for notifications. */
data class IncomingChatMessage(
    val message: DecryptedChatMessage,
    val senderName: String,
    /** Text safe to show in a notification (decrypted body, or a placeholder for invitations / locked messages). */
    val previewText: String,
)

interface ChatRepository {
    /** Ensures this device has published its E2EE public key to the server. */
    suspend fun ensurePublicKeyPublished(): Result<Unit>

    /** Fetches the public key for a target user (returns null if user has not set up E2EE). */
    suspend fun getPeerPublicKey(userId: String): Result<String?>

    /** Lists all active conversations for the current user. */
    suspend fun getConversations(): Result<List<Conversation>>

    /** Gets or creates a conversation ID for the peer user. */
    suspend fun getOrCreateConversation(otherUserId: String): Result<String>

    /** Fetches and decrypts messages for a conversation. */
    suspend fun getMessages(conversationId: String): Result<List<DecryptedChatMessage>>

    /** Encrypts and sends a plain-text message. */
    suspend fun sendTextMessage(
        conversationId: String,
        recipientUserId: String,
        text: String,
    ): Result<DecryptedChatMessage>

    /** Encrypts and sends a structured family tree invitation. */
    suspend fun sendInvitationMessage(
        conversationId: String,
        recipientUserId: String,
        invitationUrl: String,
        familyName: String?,
        token: String?,
    ): Result<DecryptedChatMessage>

    /** Marks unread messages in conversation as read. */
    suspend fun markMessagesAsRead(conversationId: String): Result<Unit>

    /**
     * Unread messages addressed to the current user created after [sinceIso] (all unread when null), oldest first,
     * already decrypted on this device. Used for new-message notifications.
     */
    suspend fun getIncomingUnreadSince(sinceIso: String?): Result<List<IncomingChatMessage>> = Result.success(emptyList())

    /** True when the server holds a password-protected backup of this device's active chat key. */
    suspend fun hasKeyBackup(): Result<Boolean> = Result.success(true)

    /** Verifies [password] against the account and uploads password-protected backups of the chat keys. */
    suspend fun protectKeysWithPassword(password: String): Result<Unit> = Result.success(Unit)
}
