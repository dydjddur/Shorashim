package com.shorashim.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.Family
import com.shorashim.core.model.FriendRequest
import com.shorashim.core.model.FriendSuggestion
import com.shorashim.core.model.FriendUser
import com.shorashim.core.model.UserProfile
import com.shorashim.data.friends.FriendRepository
import com.shorashim.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsUiState(
    val isLoading: Boolean = true,
    val friends: List<FriendUser> = emptyList(),
    val suggestions: List<FriendSuggestion> = emptyList(),
    val pendingRequests: List<FriendRequest> = emptyList(),
    val selectedFriendForTrees: FriendUser? = null,
    val friendTrees: List<Family> = emptyList(),
    val isFriendProfilePrivate: Boolean = false,
    val isLoadingFriendTrees: Boolean = false,
    val searchQuery: String = "",
    val searchResult: UserProfile? = null,
    val isSearching: Boolean = false,
    val actionMessage: String? = null,
    val errorMessage: String? = null,
)

class FriendsViewModel(
    private val friendRepository: FriendRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val friendsRes = friendRepository.getFriends()
            val suggestionsRes = friendRepository.getFriendSuggestions()
            val requestsRes = friendRepository.getPendingRequests()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    friends = friendsRes.getOrDefault(emptyList()),
                    suggestions = suggestionsRes.getOrDefault(emptyList()),
                    pendingRequests = requestsRes.getOrDefault(emptyList()),
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, searchResult = null) }
        if (query.trim().length >= 3) {
            searchUser(query.trim())
        }
    }

    private fun searchUser(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val res = profileRepository.searchUserByUsername(username)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResult = res.getOrNull(),
                )
            }
        }
    }

    fun sendFriendRequest(targetIdOrUsername: String) {
        viewModelScope.launch {
            val res = friendRepository.sendFriendRequest(targetIdOrUsername)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        actionMessage = "בקשת חברות נשלחה בהצלחה!",
                        searchResult = null,
                        searchQuery = "",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = res.exceptionOrNull()?.message ?: "שגיאה בשליחת בקשת החברות")
                }
            }
        }
    }

    fun respondToRequest(requestId: String, accept: Boolean) {
        viewModelScope.launch {
            val res = friendRepository.respondToFriendRequest(requestId, accept)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        actionMessage = if (accept) "בקשת החברות אושרה! החבר נוסף לרשימה" else "בקשת החברות נדחתה",
                    )
                }
                loadData()
            } else {
                _uiState.update {
                    it.copy(errorMessage = res.exceptionOrNull()?.message ?: "שגיאה בטיפול בבקשה")
                }
            }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            friendRepository.removeFriend(friendId)
            loadData()
        }
    }

    fun viewFriendTrees(friend: FriendUser) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedFriendForTrees = friend,
                    isLoadingFriendTrees = true,
                    friendTrees = emptyList(),
                    isFriendProfilePrivate = friend.isPrivateProfile,
                )
            }

            if (friend.isPrivateProfile) {
                _uiState.update { it.copy(isLoadingFriendTrees = false) }
                return@launch
            }

            val treesRes = friendRepository.getFriendTrees(friend.id)
            _uiState.update {
                it.copy(
                    isLoadingFriendTrees = false,
                    friendTrees = treesRes.getOrDefault(emptyList()),
                )
            }
        }
    }

    fun dismissFriendTrees() {
        _uiState.update { it.copy(selectedFriendForTrees = null, friendTrees = emptyList()) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(actionMessage = null, errorMessage = null) }
    }
}
