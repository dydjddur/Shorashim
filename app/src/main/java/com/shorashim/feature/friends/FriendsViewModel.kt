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
    val searchResults: List<UserProfile> = emptyList(),
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
    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        loadData(isSilent = false)
        startSilentPolling()
    }

    private fun startSilentPolling() {
        viewModelScope.launch {
            com.shorashim.core.common.pollWhileForeground(15_000) {
                loadData(isSilent = true)
            }
        }
    }

    fun loadData(isSilent: Boolean = false) {
        viewModelScope.launch {
            val hasData = _uiState.value.friends.isNotEmpty() ||
                _uiState.value.suggestions.isNotEmpty() ||
                _uiState.value.pendingRequests.isNotEmpty()

            if (!isSilent && !hasData) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val friendsRes = friendRepository.getFriends()
            val suggestionsRes = friendRepository.getFriendSuggestions()
            val requestsRes = friendRepository.getPendingRequests()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    friends = if (friendsRes.isSuccess) friendsRes.getOrDefault(emptyList()) else state.friends,
                    suggestions = if (suggestionsRes.isSuccess) suggestionsRes.getOrDefault(emptyList()) else state.suggestions,
                    pendingRequests = if (requestsRes.isSuccess) requestsRes.getOrDefault(emptyList()) else state.pendingRequests,
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        val q = query.trim()
        searchJob?.cancel()
        if (q.isNotEmpty()) {
            searchJob = viewModelScope.launch {
                _uiState.update { it.copy(isSearching = true) }
                // Debounce: don't hit the server on every keystroke.
                kotlinx.coroutines.delay(300)
                val res = profileRepository.searchUsers(q.removePrefix("@"))
                val list = res.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = list,
                        searchResult = list.firstOrNull(),
                    )
                }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), searchResult = null, isSearching = false) }
        }
    }

    fun sendFriendRequest(targetIdOrUsername: String) {
        viewModelScope.launch {
            val res = friendRepository.sendFriendRequest(targetIdOrUsername)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        actionMessage = "המשתמש נוסף בהצלחה לרשימת החברים שלך!",
                        searchResults = emptyList(),
                        searchResult = null,
                        searchQuery = "",
                    )
                }
                loadData(isSilent = true)
            } else {
                _uiState.update {
                    it.copy(errorMessage = res.exceptionOrNull()?.message ?: "שגיאה בהוספת חבר")
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
                loadData(isSilent = true)
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
                .onSuccess { _uiState.update { it.copy(actionMessage = "החבר הוסר מהרשימה") } }
                .onFailure { t -> _uiState.update { it.copy(errorMessage = t.message ?: "שגיאה בהסרת החבר") } }
            loadData(isSilent = true)
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
