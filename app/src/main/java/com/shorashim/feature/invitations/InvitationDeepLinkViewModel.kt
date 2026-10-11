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
                    _uiState.value = DeepLinkUiState.Error(
                        t.localizedMessage ?: "שגיאה בטעינת ההזמנה"
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
                        _uiState.value = DeepLinkUiState.Accepted
                    } else {
                        _uiState.value = DeepLinkUiState.Error(mapError(resolution.error))
                    }
                }
                .onFailure { t ->
                    _uiState.value = DeepLinkUiState.Error(
                        t.localizedMessage ?: "שגיאה בקבלת ההזמנה"
                    )
                }
        }
    }

    fun decline() {
        _uiState.value = DeepLinkUiState.Declined
    }

    private fun mapError(code: String?): String = when (code) {
        "invitation_not_found" -> "ההזמנה לא נמצאה"
        "invitation_expired"   -> "פג תוקף ההזמנה"
        "invitation_revoked"   -> "ההזמנה בוטלה על ידי שולח ההזמנה"
        "already_accepted"     -> "ההזמנה כבר התקבלה"
        "email_mismatch"       -> "ההזמנה שייכת לכתובת אימייל אחרת"
        else                   -> "שגיאה: $code"
    }
}
