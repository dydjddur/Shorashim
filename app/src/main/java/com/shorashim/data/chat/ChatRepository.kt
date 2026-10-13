package com.shorashim.data.chat

import com.shorashim.core.model.Conversation
import com.shorashim.core.model.DecryptedChatMessage
import kotlinx.coroutines.flow.Flow

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
}
