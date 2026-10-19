package com.shorashim.feature.invitations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.FamilyInvitation
import com.shorashim.core.model.InvitationResolution
import com.shorashim.data.invitations.InvitationRepository
import com.shorashim.data.family.FamilyRepository
import com.shorashim.core.model.MembershipRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InvitationsUiState(
    val pendingInvitations: List<FamilyInvitation> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,   // accepting / declining
    val error: String? = null,
    val toastMessage: String? = null,
)

class InvitationsViewModel(
    private val invitationRepository: InvitationRepository,
    private val familyRepository: FamilyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvitationsUiState(isLoading = true))
    val uiState: StateFlow<InvitationsUiState> = _uiState.asStateFlow()

    init {
        loadPendingInvitations(isSilent = false)
        startSilentPolling()
    }

    private fun startSilentPolling() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(20_000)
                loadPendingInvitations(isSilent = true)
            }
        }
    }

    fun loadPendingInvitations(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent && _uiState.value.pendingInvitations.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            invitationRepository.getMyPendingInvitations()
                .onSuccess { list ->
                    // Enrich with family names
                    val enriched = list.map { inv ->
                        val name = familyRepository.getFamily(inv.familyId)
                            .getOrNull()?.name ?: inv.familyId
                        inv.copy(familyName = name)
                    }
                    _uiState.value = _uiState.value.copy(
                        pendingInvitations = enriched,
                        isLoading = false,
                    )
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (isSilent && _uiState.value.pendingInvitations.isNotEmpty()) null else (t.localizedMessage ?: "שגיאה בטעינת ההזמנות"),
                    )
                }
        }
    }

    fun acceptInvitation(invitationId: String, onSuccess: (familyId: String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            invitationRepository.acceptInvitationById(invitationId)
                .onSuccess { resolution ->
                    if (resolution.ok) {
                        val msg = if (resolution.alreadyMember)
                            "כבר חבר/ה בעץ ${resolution.familyName.orEmpty()}"
                        else
                            "הצטרפת בהצלחה לעץ ${resolution.familyName.orEmpty()}!"
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            toastMessage = msg,
                            pendingInvitations = _uiState.value.pendingInvitations
                                .filter { it.id != invitationId },
                        )
                        resolution.familyId?.let { onSuccess(it) }
                    } else {
                        val hebrewError = mapRpcError(resolution.error)
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            error = hebrewError,
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = t.localizedMessage ?: "שגיאה בקבלת ההזמנה",
                    )
                }
        }
    }

    fun declineInvitation(invitationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            invitationRepository.declineInvitation(invitationId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        toastMessage = "ההזמנה נדחתה",
                        pendingInvitations = _uiState.value.pendingInvitations
                            .filter { it.id != invitationId },
                    )
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = t.localizedMessage ?: "שגיאה בדחיית ההזמנה",
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    private fun mapRpcError(code: String?): String = when (code) {
        "invitation_not_found" -> "ההזמנה לא נמצאה"
        "invitation_expired"   -> "פג תוקף ההזמנה"
        "invitation_revoked"   -> "ההזמנה בוטלה"
        "invitation_declined"  -> "ההזמנה כבר נדחתה"
        "already_accepted"     -> "ההזמנה כבר התקבלה"
        "email_mismatch"       -> "ההזמנה שייכת לכתובת אימייל אחרת"
        else                   -> "שגיאה בעיבוד ההזמנה: $code"
    }
}
