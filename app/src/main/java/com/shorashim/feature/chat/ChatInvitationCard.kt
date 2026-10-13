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

    val cardBg = if (isFromMe) {
        Color(0xFF1B382B) // Forest dark green
    } else {
        Color(0xFF1E2836) // Deep navy slate
    }

    val borderColor = if (isFromMe) {
        Color(0xFF22C55E).copy(alpha = 0.5f)
    } else {
        Color(0xFF38BDF8).copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFromMe) Color(0xFF22C55E).copy(alpha = 0.2f) else Color(0xFF38BDF8).copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        BoxContent(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountTree,
                                contentDescription = null,
                                tint = if (isFromMe) Color(0xFF4ADE80) else Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "הזמנה לעץ משפחה",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isFromMe) Color(0xFF86EFAC) else Color(0xFF7DD3FC),
                            ),
                        )
                        Text(
                            text = payload.familyName?.ifBlank { "עץ משפחה" } ?: "עץ משפחה",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Active status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF22C55E).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "הזמנה פעילה",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80),
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (isFromMe) {
                    "שלחת הזמנה להצטרפות לעריכת עץ זה. ההזמנה מוצפנת מקצה לקצה."
                } else {
                    "הוזמנת להצטרף לעץ המשפחה! לחץ למטה כדי לצפות בפרטי ההזמנה ולהצטרף לעץ."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFCBD5E1),
                    lineHeight = 18.sp,
                ),
            )

            Spacer(Modifier.height(12.dp))

            // Action Button
            Button(
                onClick = {
                    if (isValid && token != null) {
                        onOpenInvitation(token)
                    }
                },
                enabled = isValid && token != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFromMe) Color(0xFF16A34A) else Color(0xFF0284C7),
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFromMe) "צפה בהזמנה שנשלחה" else "פתיחת ההזמנה והצטרפות",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
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
