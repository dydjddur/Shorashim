package com.shorashim.feature.invitations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.core.model.InvitationResolution
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.Spacing

@Composable
fun InvitationDeepLinkRoute(
    viewModel: InvitationDeepLinkViewModel,
    onAccepted: (familyId: String) -> Unit,
    onDeclined: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        DeepLinkUiState.Loading -> {
            ShorashimLoadingState(modifier = modifier.fillMaxSize())
        }

        is DeepLinkUiState.Resolved -> {
            InvitationDeepLinkScreen(
                resolution = state.resolution,
                onAccept = { viewModel.accept() },
                onDecline = {
                    viewModel.decline()
                    onDeclined()
                },
                modifier = modifier,
            )
        }

        DeepLinkUiState.Accepted -> {
            // Navigate to the family that was just joined
            // We trigger onAccepted with the family ID from a remembered state —
            // but since we transition instantly, we use a LaunchedEffect-like approach:
            FeedbackScreen(
                icon = { Icon(Icons.Outlined.CheckCircle, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary) },
                message = "הצטרפת לעץ המשפחה בהצלחה! 🎉",
                buttonText = "מעבר לעץ המשפחה",
                onButton = { onNavigateHome() },
                modifier = modifier,
            )
        }

        DeepLinkUiState.Declined -> {
            FeedbackScreen(
                icon = { Icon(Icons.Outlined.Park, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                message = "דחית את ההזמנה. תמיד תוכל/י לבקש להצטרף מחדש.",
                buttonText = "חזרה לדף הבית",
                onButton = { onNavigateHome() },
                modifier = modifier,
            )
        }

        is DeepLinkUiState.Error -> {
            FeedbackScreen(
                icon = { Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error) },
                message = state.message,
                buttonText = "חזרה לדף הבית",
                onButton = { onNavigateHome() },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun InvitationDeepLinkScreen(
    resolution: InvitationResolution,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Park,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )

                Spacer(Modifier.height(Spacing.md))

                Text(
                    text = "הוזמנת לעץ משפחה:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(Spacing.xs))

                Text(
                    text = resolution.familyName ?: "עץ משפחה",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                resolution.invitedBy?.let { inviter ->
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "הוזמנת על ידי: $inviter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                resolution.role?.let { role ->
                    val hebrewRole = when (role) {
                        "CONTRIBUTOR", "EDITOR" -> "משתתף (עריכת תוכן)"
                        "VIEWER" -> "צופה (קריאה בלבד)"
                        "ADMIN" -> "מנהל"
                        else -> role
                    }
                    Text(
                        text = "תפקיד: $hebrewRole",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(Spacing.xl))

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = "אישור הצטרפות",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Spacer(Modifier.height(Spacing.sm))

                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(text = "דחיית ההזמנה")
                }
            }
        }
    }
}

@Composable
private fun FeedbackScreen(
    icon: @Composable () -> Unit,
    message: String,
    buttonText: String,
    onButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))
            Button(
                onClick = onButton,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
