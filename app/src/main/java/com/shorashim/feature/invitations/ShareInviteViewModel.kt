package com.shorashim.feature.invitations

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.FamilyInvitation
import com.shorashim.core.model.MembershipRole
import com.shorashim.data.invitations.InvitationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShareInviteUiState(
    val isLoading: Boolean = false,
    val currentInvitation: FamilyInvitation? = null,
    val error: String? = null,
    val toastMessage: String? = null,
)

class ShareInviteViewModel(
    private val familyId: String,
    private val invitationRepository: InvitationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareInviteUiState())
    val uiState: StateFlow<ShareInviteUiState> = _uiState.asStateFlow()

    /** Create a new invitation and return it for sharing. */
    fun createAndShare(
        context: Context,
        role: MembershipRole = MembershipRole.CONTRIBUTOR,
        expiresInDays: Int = GROUP_LINK_DAYS,
        familyName: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            invitationRepository.createInvitation(familyId, expiresInDays, role)
                .onSuccess { invitation ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentInvitation = invitation,
                    )
                    shareLink(context, invitation, familyName)
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = t.localizedMessage ?: "שגיאה ביצירת קישור ההזמנה",
                    )
                }
        }
    }

    fun revokeInvitation(invitationId: String) {
        viewModelScope.launch {
            invitationRepository.revokeInvitation(invitationId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        currentInvitation = null,
                        toastMessage = "קישור ההזמנה בוטל",
                    )
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(
                        error = t.localizedMessage ?: "שגיאה בביטול ההזמנה",
                    )
                }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearToast() { _uiState.value = _uiState.value.copy(toastMessage = null) }

    companion object {
        /** A shared family link stays valid for a month so the whole family has time to join. */
        const val GROUP_LINK_DAYS = 30

        fun shareLink(context: Context, invitation: FamilyInvitation, familyName: String? = null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "הזמנה לעץ משפחה בשורשים")
                val tree = familyName?.takeIf { it.isNotBlank() }?.let { " \"$it\"" }.orEmpty()
                putExtra(
                    Intent.EXTRA_TEXT,
                    "\uD83C\uDF33 הוזמנתם להצטרף לעץ המשפחה$tree באפליקציית שורשים!\n" +
                        "הקישור מיועד לכל בני המשפחה — לחצו עליו, התחברו ואשרו את ההזמנה:\n${invitation.shareUrl}",
                )
            }
            context.startActivity(Intent.createChooser(intent, "שיתוף קישור הזמנה"))
        }
    }
}
