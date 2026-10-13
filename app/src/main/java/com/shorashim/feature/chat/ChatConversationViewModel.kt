package com.shorashim.feature.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.DecryptedChatMessage
import com.shorashim.core.model.Family
import com.shorashim.core.model.MembershipRole
import com.shorashim.data.chat.ChatRepository
import com.shorashim.data.family.FamilyRepository
import com.shorashim.data.invitations.InvitationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatConversationUiState(
    val conversationId: String? = null,
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserAvatar: String? = null,
    val isOtherUserOnline: Boolean = false,
    val messages: List<DecryptedChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isTreePickerOpen: Boolean = false,
    val myFamilies: List<Family> = emptyList(),
    val error: String? = null,
    val toastMessage: String? = null,
)

class ChatConversationViewModel(
    val otherUserId: String,
    val otherUserName: String = "",
    private val chatRepository: ChatRepository,
    private val invitationRepository: InvitationRepository,
    private val familyRepository: FamilyRepository,
    private val supabaseClient: SupabaseClient? = null,
) : ViewModel() {

    companion object {
        private const val TAG = "ChatConversationVM"
    }

    private val _uiState = MutableStateFlow(
        ChatConversationUiState(
            otherUserId = otherUserId,
            otherUserName = otherUserName,
            isLoading = true,
        )
    )
    val uiState: StateFlow<ChatConversationUiState> = _uiState.asStateFlow()

    private var realtimeChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null

    init {
        initializeConversation()
    }

    fun initializeConversation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Ensure our own public key is published
            chatRepository.ensurePublicKeyPublished()

            // Resolve or create conversation
            val convResult = chatRepository.getOrCreateConversation(otherUserId)
            if (convResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = convResult.exceptionOrNull()?.message ?: "שגיאה ביצירת שיחה",
                )
                return@launch
            }

            val convId = convResult.getOrThrow()
            _uiState.value = _uiState.value.copy(
                conversationId = convId,
                isLoading = false,
            )

            loadMessages(convId)
            setupRealtime(convId)
        }
    }

    private fun loadMessages(convId: String) {
        viewModelScope.launch {
            val result = chatRepository.getMessages(convId)
            if (result.isSuccess) {
                val msgs = result.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(
                    messages = msgs,
                    isLoading = false,
                )
                chatRepository.markMessagesAsRead(convId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(inputText = newText)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        val convId = _uiState.value.conversationId ?: return
        if (text.isBlank() || _uiState.value.isSending) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, error = null)
            val result = chatRepository.sendTextMessage(
                conversationId = convId,
                recipientUserId = otherUserId,
                text = text,
            )

            if (result.isSuccess) {
                val sentMsg = result.getOrThrow()
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + sentMsg,
                    inputText = "",
                    isSending = false,
                )
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "שגיאה בשליחת הודעה מוצפנת"
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = errorMsg,
                )
            }
        }
    }

    fun openTreePicker() {
        viewModelScope.launch {
            val fams = familyRepository.getMyFamilies().getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                myFamilies = fams,
                isTreePickerOpen = true,
            )
        }
    }

    fun dismissTreePicker() {
        _uiState.value = _uiState.value.copy(isTreePickerOpen = false)
    }

    fun sendFamilyInvitation(family: Family) {
        val convId = _uiState.value.conversationId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, isTreePickerOpen = false)

            val inviteResult = invitationRepository.createInvitation(
                familyId = family.id,
                role = MembershipRole.CONTRIBUTOR,
            )

            if (inviteResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = "נכשל ביצירת קישור הזמנה לעץ: ${inviteResult.exceptionOrNull()?.message}",
                )
                return@launch
            }

            val invite = inviteResult.getOrThrow()
            val sendResult = chatRepository.sendInvitationMessage(
                conversationId = convId,
                recipientUserId = otherUserId,
                invitationUrl = invite.shareUrl,
                familyName = family.name,
                token = invite.token,
            )

            if (sendResult.isSuccess) {
                val sentMsg = sendResult.getOrThrow()
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + sentMsg,
                    isSending = false,
                    toastMessage = "ההזמנה לעץ נשלחה בהצלחה בשיחה המוצפנת!",
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = sendResult.exceptionOrNull()?.message ?: "שגיאה בשליחת ההזמנה בשיחה",
                )
            }
        }
    }

    private fun setupRealtime(convId: String) {
        val client = supabaseClient ?: return
        val isUuid = convId.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
        if (!isUuid) return
        teardownRealtime()

        viewModelScope.launch {
            try {
                val channel = client.realtime.channel("chat_room_$convId")
                realtimeChannel = channel

                val messageFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "chat_messages"
                    filter(FilterOperation("conversation_id", FilterOperator.EQ, convId))
                }

                realtimeJob = launch {
                    messageFlow.collect {
                        refreshMessagesSilently(convId)
                    }
                }

                channel.subscribe()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to setup realtime chat subscription: ${t.message}")
            }
        }
    }

    private fun refreshMessagesSilently(convId: String) {
        viewModelScope.launch {
            val result = chatRepository.getMessages(convId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(messages = result.getOrDefault(emptyList()))
                chatRepository.markMessagesAsRead(convId)
            }
        }
    }

    private fun teardownRealtime() {
        realtimeJob?.cancel()
        realtimeJob = null
        val client = supabaseClient
        val channel = realtimeChannel
        if (client != null && channel != null) {
            viewModelScope.launch {
                try {
                    client.realtime.removeChannel(channel)
                } catch (_: Throwable) {}
            }
        }
        realtimeChannel = null
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, toastMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        teardownRealtime()
    }
}
