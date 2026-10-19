package com.shorashim.feature.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.core.common.InvitationUrlValidator
import com.shorashim.core.model.InvitationMessagePayload

/**
 * Rich invitation card component rendered inside chat when a message
 * represents a valid Shorashim family-tree invitation.
 */
@Composable
fun ChatInvitationCard(
    payload: InvitationMessagePayload,
    isFromMe: Boolean,
    onOpenInvitation: (token: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val token = payload.token ?: InvitationUrlValidator.extractToken(payload.url)
    val isValid = !token.isNullOrBlank() && InvitationUrlValidator.isValidInvitationUrl(payload.url)

    val scheme = MaterialTheme.colorScheme
    val extra = com.shorashim.core.ui.theme.ShorashimTheme.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, if (isFromMe) scheme.primary.copy(alpha = 0.35f) else scheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.shorashim.core.ui.component.IconTile(
                    icon = Icons.Outlined.AccountTree,
                    size = 40.dp,
                    cornerRadius = 12.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "הזמנה לעץ משפחה",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = payload.familyName?.ifBlank { "עץ משפחה" } ?: "עץ משפחה",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Only claim the invitation is active when the link is actually usable.
                if (isValid) {
                    com.shorashim.core.ui.component.StatusPill(
                        text = "פעילה",
                        icon = Icons.Outlined.CheckCircle,
                        container = extra.successContainer,
                        content = extra.success,
                    )
                } else {
                    com.shorashim.core.ui.component.StatusPill(
                        text = "קישור לא תקין",
                        container = scheme.errorContainer,
                        content = scheme.onErrorContainer,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (isFromMe) {
                    "שלחת הזמנה להצטרפות לעריכת עץ זה. ההזמנה מוצפנת מקצה לקצה."
                } else {
                    "הוזמנת להצטרף לעץ המשפחה! פתחו את ההזמנה כדי לראות את הפרטים ולהצטרף."
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            val buttonContent: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFromMe) "צפייה בהזמנה" else "פתיחת ההזמנה והצטרפות",
                    fontWeight = FontWeight.Bold,
                )
            }
            if (isFromMe) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { if (token != null) onOpenInvitation(token) },
                    enabled = isValid && token != null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    content = buttonContent,
                )
            } else {
                Button(
                    onClick = { if (token != null) onOpenInvitation(token) },
                    enabled = isValid && token != null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    content = buttonContent,
                )
            }
        }
    }
}

@Composable
private fun BoxContent(
    contentAlignment: Alignment,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        contentAlignment = contentAlignment,
        modifier = modifier,
    ) {
        content()
    }
}
