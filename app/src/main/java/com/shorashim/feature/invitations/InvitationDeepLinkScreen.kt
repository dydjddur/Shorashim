package com.shorashim.feature.invitations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.core.model.InvitationResolution
import com.shorashim.core.ui.component.IconTile
import com.shorashim.core.ui.component.ShorashimCard
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.StatusPill
import com.shorashim.core.ui.component.Spacing
import com.shorashim.core.ui.theme.ShorashimTheme

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
            FeedbackScreen(
                icon = Icons.Outlined.CheckCircle,
                tint = ShorashimTheme.colors.success,
                container = ShorashimTheme.colors.successContainer,
                message = "הצטרפת לעץ המשפחה בהצלחה!",
                buttonText = "מעבר לעץ המשפחה",
                onButton = {
                    // Previously this always went home, so the button never opened the tree that was just joined.
                    val familyId = viewModel.acceptedFamilyId
                    if (familyId != null) onAccepted(familyId) else onNavigateHome()
                },
                modifier = modifier,
            )
        }

        DeepLinkUiState.Declined -> {
            FeedbackScreen(
                icon = Icons.Outlined.Park,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                container = MaterialTheme.colorScheme.surfaceVariant,
                message = "דחית את ההזמנה. תמיד תוכל/י לבקש להצטרף מחדש.",
                buttonText = "חזרה לדף הבית",
                onButton = { onNavigateHome() },
                modifier = modifier,
            )
        }

        is DeepLinkUiState.Error -> {
            FeedbackScreen(
                icon = Icons.Outlined.ErrorOutline,
                tint = MaterialTheme.colorScheme.error,
                container = MaterialTheme.colorScheme.errorContainer,
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
    // Centered when it fits, scrollable when it doesn't (small phones / landscape).
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val minHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .padding(horizontal = 20.dp, vertical = Spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
        ShorashimCard(
            modifier = Modifier.widthIn(max = 480.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconTile(
                    icon = Icons.Outlined.Park,
                    size = 72.dp,
                    iconSize = 36.dp,
                    cornerRadius = 24.dp,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "הוזמנת להצטרף לעץ משפחה",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = resolution.familyName ?: "עץ משפחה",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                resolution.invitedBy?.let { inviter ->
                    Spacer(Modifier.height(Spacing.sm))
                    StatusPill(
                        text = "הוזמנת על ידי $inviter",
                        icon = Icons.Outlined.PersonOutline,
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                resolution.role?.let { role ->
                    Spacer(Modifier.height(Spacing.sm))
                    StatusPill(
                        text = roleLabel(role),
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Spacer(Modifier.height(Spacing.xl))

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(text = "אישור הצטרפות")
                }

                Spacer(Modifier.height(Spacing.sm))

                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(text = "דחיית ההזמנה", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        }
    }
}

internal fun roleLabel(role: String): String = when (role) {
    "CONTRIBUTOR", "EDITOR" -> "משתתף · עריכת תוכן"
    "VIEWER" -> "צופה · קריאה בלבד"
    "ADMIN" -> "מנהל"
    "OWNER" -> "מנהל-על"
    else -> role
}

@Composable
private fun FeedbackScreen(
    icon: ImageVector,
    tint: Color,
    container: Color,
    message: String,
    buttonText: String,
    onButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconTile(icon = icon, tint = tint, container = container, size = 80.dp, iconSize = 40.dp, cornerRadius = 26.dp)
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = onButton,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(buttonText)
        }
    }
}
