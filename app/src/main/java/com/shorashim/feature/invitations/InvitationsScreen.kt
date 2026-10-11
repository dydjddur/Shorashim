package com.shorashim.feature.invitations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.core.model.FamilyInvitation
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.Spacing

@Composable
fun InvitationsRoute(
    viewModel: InvitationsViewModel,
    onNavigateToFamily: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.loadPendingInvitations()
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
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        InvitationsScreen(
            uiState = uiState,
            onAccept = { inv ->
                viewModel.acceptInvitation(inv.id) { familyId ->
                    onNavigateToFamily(familyId)
                }
            },
            onDecline = { inv -> viewModel.declineInvitation(inv.id) },
            onRefresh = viewModel::loadPendingInvitations,
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
        uiState.pendingInvitations.isEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(Spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Mail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = "אין הזמנות ממתינות",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "כאן יופיעו הזמנות שתקבל/י לעצי משפחה של אחרים",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                contentPadding = PaddingValues(vertical = Spacing.md),
            ) {
                item {
                    Text(
                        text = "הזמנות לעץ משפחתי",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                }

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

@Composable
fun InvitationCard(
    invitation: FamilyInvitation,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Park,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "הוזמנת לעץ משפחה:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = invitation.familyName.ifBlank { "עץ משפחה" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            val roleText = when (invitation.role.name) {
                "CONTRIBUTOR", "EDITOR" -> "משתתף (עריכת תוכן)"
                "VIEWER" -> "צופה (קריאה בלבד)"
                "ADMIN" -> "מנהל"
                "OWNER" -> "מנהל-על"
                else -> invitation.role.name
            }
            Text(
                text = "תפקיד: $roleText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            invitation.expiresAt?.let { expiresAt ->
                val readableDate = expiresAt.take(10) // YYYY-MM-DD
                Text(
                    text = "בתוקף עד: $readableDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            Spacer(Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Decline button
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("דחיית ההזמנה")
                }

                // Accept button
                Button(
                    onClick = onAccept,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("אישור הצטרפות")
                    }
                }
            }
        }
    }
}
