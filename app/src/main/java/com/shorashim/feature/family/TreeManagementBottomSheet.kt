package com.shorashim.feature.family

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
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.R
import com.shorashim.core.model.Family
import com.shorashim.core.model.MembershipRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeManagementBottomSheet(
    family: Family,
    onPublish: () -> Unit,
    onUnpublish: () -> Unit,
    onOpenMembers: () -> Unit,
    onShareInvite: () -> Unit = {},
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = family.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.tree_management_sheet_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Current User Role Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (family.currentUserRole) {
                        MembershipRole.OWNER -> Color(0xFFD4AF37).copy(alpha = 0.2f)
                        MembershipRole.ADMIN -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = when (family.currentUserRole) {
                                MembershipRole.OWNER -> Color(0xFFD4AF37)
                                MembershipRole.ADMIN -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = when (family.currentUserRole) {
                                MembershipRole.OWNER -> "מנהל-על (בעלים)"
                                MembershipRole.ADMIN -> "מנהל"
                                MembershipRole.CONTRIBUTOR -> "משתתף"
                                MembershipRole.VIEWER -> "צופה"
                                else -> ""
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Draft / Publish Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (family.isDraft) {
                        Color(0xFFF9A825).copy(alpha = 0.12f)
                    } else {
                        Color(0xFF43A047).copy(alpha = 0.12f)
                    },
                ),
                border = BorderStroke(
                    1.dp,
                    if (family.isDraft) Color(0xFFF9A825) else Color(0xFF43A047),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (family.isDraft) "סטטוס: טיוטה" else "סטטוס: פורסם ומשותף",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (family.isDraft) {
                                "העץ נראה רק לך. לחצו להעלאה כדי לאפשר שיתוף."
                            } else {
                                "העץ פעיל וזמין לכל המשתתפים שהוזמנו."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (family.canManageMembers) {
                        if (family.isDraft) {
                            Button(
                                onClick = onPublish,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("העלאה", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onUnpublish,
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(Icons.Outlined.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.tree_unpublish_action))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Management Action Buttons
            if (family.canManageMembers) {
                Button(
                    onClick = onShareInvite,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.invitation_share_action), fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = onOpenMembers,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Group, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.tree_manage_members), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(10.dp))

            if (family.canManageMembers) {
                OutlinedButton(
                    onClick = onRenameClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.family_rename_dialog_title), fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(10.dp))
            }

            // Delete Tree — OWNER only
            if (family.isOwner) {
                Button(
                    onClick = onDeleteClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.family_delete_action), fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.members_no_permission_delete),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}
