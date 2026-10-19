package com.shorashim.feature.invitations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.InvitationResolution
import com.shorashim.data.invitations.InvitationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DeepLinkUiState {
    data object Loading : DeepLinkUiState
    data class Resolved(val resolution: InvitationResolution) : DeepLinkUiState
    data class Error(val message: String) : DeepLinkUiState
    data object Accepted : DeepLinkUiState
    data object Declined : DeepLinkUiState
}

class InvitationDeepLinkViewModel(
    private val token: String,
    private val invitationRepository: InvitationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeepLinkUiState>(DeepLinkUiState.Loading)
    val uiState: StateFlow<DeepLinkUiState> = _uiState.asStateFlow()

    /** Family joined by [accept]; lets the success screen open that tree instead of just going home. */
    var acceptedFamilyId: String? = null
        private set

    init {
        resolveToken()
    }

    private fun resolveToken() {
        viewModelScope.launch {
            _uiState.value = DeepLinkUiState.Loading
            invitationRepository.resolveInvitationByToken(token)
                .onSuccess { resolution ->
                    if (resolution.ok) {
                        _uiState.value = DeepLinkUiState.Resolved(resolution)
                    } else {
                        _uiState.value = DeepLinkUiState.Error(mapError(resolution.error))
                    }
                }
                .onFailure { t ->
                    android.util.Log.e("InvitationDeepLink", "resolveInvitationByToken failed, falling back to join confirmation", t)
                    // If resolve metadata fails (e.g. backend missing profiles relation),
                    // allow user to proceed with acceptance rather than blocking them with a raw error
                    _uiState.value = DeepLinkUiState.Resolved(
                        InvitationResolution(
                            ok = true,
                            familyName = "עץ משפחה",
                            role = "CONTRIBUTOR",
                        ),
                    )
                }
        }
    }

    fun accept() {
        viewModelScope.launch {
            _uiState.value = DeepLinkUiState.Loading
            invitationRepository.acceptInvitationByToken(token)
                .onSuccess { resolution ->
                    if (resolution.ok) {
                        acceptedFamilyId = resolution.familyId
                        _uiState.value = DeepLinkUiState.Accepted
                    } else {
                        _uiState.value = DeepLinkUiState.Error(mapError(resolution.error))
                    }
                }
                .onFailure { t ->
                    android.util.Log.e("InvitationDeepLink", "acceptInvitationByToken failed", t)
                    _uiState.value = DeepLinkUiState.Error(
                        sanitizeErrorMessage(t.localizedMessage)
                    )
                }
        }
    }

    fun decline() {
        _uiState.value = DeepLinkUiState.Declined
    }

    private fun sanitizeErrorMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "שגיאה בקבלת ההזמנה"
        if (raw.contains("relation", ignoreCase = true) ||
            raw.contains("URL:", ignoreCase = true) ||
            raw.contains("Headers:", ignoreCase = true) ||
            raw.contains("Bearer", ignoreCase = true)
        ) {
            return "שגיאה בחיבור לשרת. אנא נסה שוב מאוחר יותר."
        }
        return raw
    }

    private fun mapError(code: String?): String = when (code) {
        "invitation_not_found" -> "ההזמנה לא נמצאה"
        "invitation_expired"   -> "פג תוקף ההזמנה"
        "invitation_revoked"   -> "ההזמנה בוטלה על ידי שולח ההזמנה"
        "already_accepted"     -> "ההזמנה כבר התקבלה"
        "invitation_full"      -> "קישור ההזמנה הגיע למספר המצטרפים המרבי"
        "not_authenticated"    -> "יש להתחבר לחשבון כדי לאשר את ההזמנה"
        "email_mismatch"       -> "ההזמנה שייכת לכתובת אימייל אחרת"
        else                   -> if (code.isNullOrBlank()) "שגיאה בהצטרפות" else "שגיאה: $code"
    }
}
