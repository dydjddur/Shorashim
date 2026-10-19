package com.shorashim.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.Conversation
import com.shorashim.core.model.FriendUser
import com.shorashim.data.chat.ChatRepository
import com.shorashim.data.friends.FriendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatListUiState(
    val conversations: List<Conversation> = emptyList(),
    val friends: List<FriendUser> = emptyList(),
    val appUsers: List<com.shorashim.core.model.FriendSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val isNewChatDialogOpen: Boolean = false,
    val error: String? = null,
)

class ChatListViewModel(
    private val chatRepository: ChatRepository,
    private val friendRepository: FriendRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState(isLoading = true))
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
        // New conversations / unread counters from other people appear without the user having to pull to refresh.
        viewModelScope.launch {
            com.shorashim.core.common.pollWhileForeground(8_000) {
                chatRepository.getConversations().getOrNull()?.let { list ->
                    if (list != _uiState.value.conversations) {
                        _uiState.value = _uiState.value.copy(conversations = list)
                    }
                }
            }
        }
    }

    fun loadConversations(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent && _uiState.value.conversations.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            chatRepository.ensurePublicKeyPublished()
            val result = chatRepository.getConversations()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    conversations = result.getOrDefault(emptyList()),
                    isLoading = false,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (isSilent && _uiState.value.conversations.isNotEmpty()) null else result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun openNewChatDialog() {
        viewModelScope.launch {
            val friends = friendRepository.getFriends().getOrDefault(emptyList())
            val appUsers = friendRepository.getFriendSuggestions().getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                friends = friends,
                appUsers = appUsers,
                isNewChatDialogOpen = true,
            )
        }
    }

    fun dismissNewChatDialog() {
        _uiState.value = _uiState.value.copy(isNewChatDialogOpen = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
