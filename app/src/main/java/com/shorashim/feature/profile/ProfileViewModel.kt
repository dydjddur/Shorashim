package com.shorashim.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.Family
import com.shorashim.core.model.UserProfile
import com.shorashim.data.family.FamilyRepository
import com.shorashim.data.friends.FriendRepository
import com.shorashim.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val families: List<Family> = emptyList(),
    val friendsCount: Int = 0,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val familyRepository: FamilyRepository,
    private val friendRepository: FriendRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val profileRes = profileRepository.getProfile()
            val familiesRes = familyRepository.getMyFamilies()
            val friendsRes = friendRepository.getFriends()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    profile = profileRes.getOrNull() ?: state.profile,
                    families = familiesRes.getOrDefault(emptyList()),
                    friendsCount = friendsRes.getOrDefault(emptyList()).size,
                    errorMessage = profileRes.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun updateProfile(fullName: String, displayName: String, bio: String?) {
        val current = _uiState.value.profile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            val res = profileRepository.updateProfile(
                fullName = fullName,
                displayName = displayName,
                avatarUrl = current.avatarUrl,
                isPrivate = current.isPrivate,
                bio = bio,
            )
            _uiState.update { state ->
                if (res.isSuccess) {
                    state.copy(
                        isSaving = false,
                        profile = res.getOrNull(),
                        successMessage = "הפרופיל עודכן בהצלחה",
                    )
                } else {
                    state.copy(
                        isSaving = false,
                        errorMessage = res.exceptionOrNull()?.message ?: "שגיאה בעדכון הפרופיל",
                    )
                }
            }
        }
    }

    fun updateAvatar(avatarBase64: String?) {
        val current = _uiState.value.profile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            val res = profileRepository.updateProfile(
                fullName = current.fullName,
                displayName = current.displayName,
                avatarUrl = avatarBase64,
                isPrivate = current.isPrivate,
                bio = current.bio,
            )
            _uiState.update { state ->
                if (res.isSuccess) {
                    state.copy(
                        isSaving = false,
                        profile = res.getOrNull(),
                        successMessage = "תמונת הפרופיל עודכנה בהצלחה",
                    )
                } else {
                    state.copy(
                        isSaving = false,
                        errorMessage = res.exceptionOrNull()?.message ?: "שגיאה בעדכון תמונת הפרופיל",
                    )
                }
            }
        }
    }

    fun togglePrivateAccountMode(isPrivate: Boolean) {
        val current = _uiState.value.profile ?: return
        _uiState.update { it.copy(profile = current.copy(isPrivate = isPrivate)) }

        viewModelScope.launch {
            val res = profileRepository.setPrivateAccountMode(isPrivate)
            if (res.isFailure) {
                // Revert on failure
                _uiState.update { it.copy(profile = current, errorMessage = "שגיאה בשינוי מצב פרטיות") }
            } else {
                _uiState.update {
                    it.copy(
                        successMessage = if (isPrivate) "מצב פרטי הופעל (העצים והחברים מוסתרים)" else "מצב פרטי בוטל",
                    )
                }
            }
        }
    }

    fun toggleTreePrivacy(familyId: String, makePrivate: Boolean) {
        viewModelScope.launch {
            val res = familyRepository.setFamilyPrivacy(familyId, isPrivate = makePrivate)
            if (res.isSuccess) {
                val updatedFamily = res.getOrNull()
                _uiState.update { state ->
                    state.copy(
                        families = state.families.map {
                            if (it.id == familyId && updatedFamily != null) it.withUpdate(updatedFamily) else it
                        },
                        successMessage = if (makePrivate) "העץ הוגדר כפרטי והוסתר מהקהילה" else "העץ הוגדר כקהילתי ופורסם בענן",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = res.exceptionOrNull()?.message ?: "שגיאה בשינוי פרטיות העץ")
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
