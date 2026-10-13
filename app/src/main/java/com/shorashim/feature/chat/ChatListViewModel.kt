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
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
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
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun openNewChatDialog() {
        viewModelScope.launch {
            val friends = friendRepository.getFriends().getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                friends = friends,
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
