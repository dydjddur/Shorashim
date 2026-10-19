package com.shorashim.feature.invitations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.core.common.TimeFormat
import com.shorashim.core.model.FamilyInvitation
import com.shorashim.core.ui.component.IconTile
import com.shorashim.core.ui.component.ScreenHeader
import com.shorashim.core.ui.component.ShorashimCard
import com.shorashim.core.ui.component.ShorashimEmptyState
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.ShorashimSnackbarHost
import com.shorashim.core.ui.component.StatusPill
import com.shorashim.core.ui.component.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InvitationsRoute(
    viewModel: InvitationsViewModel,
    onNavigateToFamily: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.loadPendingInvitations(isSilent = true)
    }

    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(uiState.error) {
        val err = uiState.error
        if (err != null) {
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
        snackbarHost = { ShorashimSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        InvitationsScreen(
            uiState = uiState,
            onAccept = { inv ->
                viewModel.acceptInvitation(inv.id) { familyId ->
                    onNavigateToFamily(familyId)
                }
            },
            onDecline = { inv -> viewModel.declineInvitation(inv.id) },
            onRefresh = { viewModel.loadPendingInvitations() },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun InvitationsScreen(
    uiState: InvitationsUiState,
    onAccept: (FamilyInvitation) -> Unit,
    onDecline: (FamilyInvitation) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> ShorashimLoadingState(modifier = modifier.fillMaxSize())
        uiState.pendingInvitations.isEmpty() -> ShorashimEmptyState(
            title = "אין הזמנות ממתינות",
            message = "כאן יופיעו הזמנות שתקבל/י לעצי משפחה של אחרים.",
            icon = Icons.Outlined.Mail,
            modifier = modifier,
            action = {
                // onRefresh was previously never wired to anything on this screen.
                OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("רענון")
                }
            },
        )
        else -> androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxSize()) {
            ScreenHeader(
                title = "הזמנות",
                subtitle = "${uiState.pendingInvitations.size} הזמנות ממתינות לתשובתך",
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = Spacing.xl),
            ) {
            items(uiState.pendingInvitations, key = { it.id }) { invitation ->
                InvitationCard(
                    invitation = invitation,
                    isProcessing = uiState.isProcessing,
                    onAccept = { onAccept(invitation) },
                    onDecline = { onDecline(invitation) },
                )
            }
            }
        }
    }
}

private val expiryFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun InvitationCard(
    invitation: FamilyInvitation,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The view model falls back to the raw family id when the name can't be fetched; never show a UUID.
    val familyName = invitation.familyName.takeIf { it.isNotBlank() && it != invitation.familyId } ?: "עץ משפחה"
    val expiry = remember(invitation.expiresAt) {
        TimeFormat.parseInstant(invitation.expiresAt)
            ?.atZone(ZoneId.systemDefault())
            ?.format(expiryFormatter)
            ?: invitation.expiresAt?.take(10)
    }

    ShorashimCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = Icons.Outlined.Park)
            Spacer(Modifier.width(14.dp))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "הוזמנת לעץ משפחה",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = familyName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(
                text = roleLabel(invitation.role.name),
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (expiry != null) {
                StatusPill(
                    text = "בתוקף עד $expiry",
                    icon = Icons.Outlined.Schedule,
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Button(
                onClick = onAccept,
                enabled = !isProcessing,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("אישור הצטרפות", maxLines = 1)
                }
            }
            OutlinedButton(
                onClick = onDecline,
                enabled = !isProcessing,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("דחייה", maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
