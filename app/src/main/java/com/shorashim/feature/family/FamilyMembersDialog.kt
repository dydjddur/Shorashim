package com.shorashim.feature.family

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Shield
import com.shorashim.core.ui.component.ShorashimDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.R
import com.shorashim.core.model.FamilyMember
import com.shorashim.core.model.MembershipRole
import com.shorashim.core.model.UserProfile
import kotlinx.coroutines.delay

@Composable
fun FamilyMembersDialog(
    members: List<FamilyMember>,
    canManageRoles: Boolean,
    isOwner: Boolean,
    onSearchUser: suspend (username: String) -> UserProfile?,
    onInviteMember: (username: String, role: MembershipRole) -> Unit,
    onUpdateRole: (userId: String, role: MembershipRole) -> Unit,
    onRemoveMember: (userId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var isInviting by remember { mutableStateOf(false) }
    var inviteUsername by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf(MembershipRole.CONTRIBUTOR) }
    var searchedUser by remember { mutableStateOf<UserProfile?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Debounced search as user types username
    LaunchedEffect(inviteUsername) {
        val clean = inviteUsername.trim().removePrefix("@")
        if (clean.length < 3) {
            searchedUser = null
            searchError = null
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        searchError = null
        delay(400) // debounce
        val user = onSearchUser(clean)
        isSearching = false
        if (user != null) {
            searchedUser = user
            searchError = when {
                members.any { it.userId == user.id } -> "המשתמש כבר חבר בעץ זה"
                members.any { it.isCurrentUser && it.userId == user.id } -> "לא ניתן להזמין את עצמך"
                else -> null
            }
        } else {
            searchedUser = null
            searchError = "שם המשתמש לא נמצא במערכת"
        }
    }

    ShorashimDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.members_title),
        subtitle = "ניהול הרשאות ושיתוף משתמשים בעץ המשפחה",
        heroIcon = Icons.Outlined.Shield,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                if (isInviting) {
                    // Direct Username Invite Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "הזמנת חבר משפחה לפי שם משתמש",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = inviteUsername,
                                onValueChange = { inviteUsername = it },
                                label = { Text(stringResource(R.string.auth_username_label)) },
                                placeholder = { Text(stringResource(R.string.members_search_username_hint)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.AlternateEmail, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    } else if (searchedUser != null && searchError == null) {
                                        Icon(
                                            Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            // Error or Preview Card
                            if (searchError != null) {
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = searchError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            } else if (searchedUser != null) {
                                Spacer(Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Outlined.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = searchedUser!!.displayName.ifBlank { searchedUser!!.fullName },
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                            Text(
                                                text = "@${searchedUser!!.username}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Role Selection
                            Text(
                                text = stringResource(R.string.members_role_label),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val roles = if (isOwner) {
                                    listOf(MembershipRole.ADMIN, MembershipRole.CONTRIBUTOR, MembershipRole.VIEWER)
                                } else {
                                    listOf(MembershipRole.CONTRIBUTOR, MembershipRole.VIEWER)
                                }

                                roles.forEach { role ->
                                    val isSelected = inviteRole == role
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { inviteRole = role },
                                    ) {
                                        Text(
                                            text = when (role) {
                                                MembershipRole.ADMIN -> "מנהל"
                                                MembershipRole.CONTRIBUTOR -> "משתתף"
                                                MembershipRole.VIEWER -> "צופה"
                                                else -> ""
                                            },
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { isInviting = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val u = searchedUser
                                        if (u != null && searchError == null) {
                                            onInviteMember(u.username ?: inviteUsername.trim().removePrefix("@"), inviteRole)
                                            inviteUsername = ""
                                            searchedUser = null
                                            isInviting = false
                                        }
                                    },
                                    enabled = searchedUser != null && searchError == null,
                                ) {
                                    Text("שליחת הזמנה")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else if (canManageRoles) {
                    Button(
                        onClick = { isInviting = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("הזמנת חבר משפחה לפי שם משתמש")
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Members List
                if (members.isEmpty()) {
                    Text(
                        text = "אין משתתפים נוספים עדיין.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(members, key = { it.userId }) { member ->
                            MemberItemRow(
                                member = member,
                                canManage = canManageRoles,
                                isOwner = isOwner,
                                onRoleSelected = { newRole -> onUpdateRole(member.userId, newRole) },
                                onRemove = { onRemoveMember(member.userId) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun MemberItemRow(
    member: FamilyMember,
    canManage: Boolean,
    isOwner: Boolean,
    onRoleSelected: (MembershipRole) -> Unit,
    onRemove: () -> Unit,
) {
    var roleMenuOpen by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = when (member.role) {
                    MembershipRole.OWNER -> Color(0xFFD4AF37).copy(alpha = 0.2f)
                    MembershipRole.ADMIN -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when (member.role) {
                            MembershipRole.OWNER -> Color(0xFFD4AF37)
                            MembershipRole.ADMIN -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                val headerName = member.displayName?.ifBlank { null }
                    ?: member.username?.let { "@$it" }
                    ?: "חבר משפחה"

                Text(
                    text = if (member.isCurrentUser) "$headerName (אתה)" else headerName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (!member.username.isNullOrBlank()) {
                    Text(
                        text = "@${member.username}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Text(
                    text = when (member.role) {
                        MembershipRole.OWNER -> stringResource(R.string.role_owner)
                        MembershipRole.ADMIN -> stringResource(R.string.role_admin)
                        MembershipRole.CONTRIBUTOR -> stringResource(R.string.role_contributor)
                        MembershipRole.VIEWER -> stringResource(R.string.role_viewer)
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (canManage && member.role != MembershipRole.OWNER && !member.isCurrentUser) {
                Box {
                    IconButton(onClick = { roleMenuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "שנה תפקיד",
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = roleMenuOpen,
                        onDismissRequest = { roleMenuOpen = false },
                    ) {
                        if (isOwner) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.role_admin)) },
                                onClick = {
                                    roleMenuOpen = false
                                    onRoleSelected(MembershipRole.ADMIN)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.role_contributor)) },
                            onClick = {
                                roleMenuOpen = false
                                onRoleSelected(MembershipRole.CONTRIBUTOR)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.role_viewer)) },
                            onClick = {
                                roleMenuOpen = false
                                onRoleSelected(MembershipRole.VIEWER)
                            },
                        )
                    }
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "הסר",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
