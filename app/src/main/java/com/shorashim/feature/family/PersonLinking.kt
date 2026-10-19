package com.shorashim.feature.family

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import com.shorashim.core.ui.component.ShorashimConfirmDialog
import com.shorashim.core.ui.component.ShorashimDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shorashim.R
import com.shorashim.core.common.HebrewDates
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.FriendUser
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.model.MutualFamilyConnection
import com.shorashim.core.model.PartialDate
import com.shorashim.core.model.Person
import com.shorashim.core.model.Relationship
import com.shorashim.core.model.RelationshipType
import com.shorashim.core.model.UserProfile
import com.shorashim.core.ui.component.PersonAvatar
import com.shorashim.core.ui.component.Spacing
import kotlinx.coroutines.launch

/** Actions the "link this tree record to an app account" section needs; supplied by the edit-person screen. */
class PersonLinkingActions(
    val onLinkUser: (userId: String) -> Unit,
    val onUnlinkUser: () -> Unit,
    val onSearchUser: suspend (username: String) -> UserProfile?,
    val onGetFriends: (suspend () -> List<FriendUser>)?,
    val onGetMutualConnections: (suspend (userId: String) -> List<MutualFamilyConnection>)?,
)

/**
 * "Link to an app account" block of the edit-person screen: shows who the record is linked to (with unlink and a
 * shared-trees view) or a button that opens the account search / friends picker.
 */
@Composable
fun PersonLinkSection(
    person: Person,
    actions: PersonLinkingActions,
    modifier: Modifier = Modifier,
) {
    var showLinkDialog by remember { mutableStateOf(false) }
    var showUnlinkConfirmDialog by remember { mutableStateOf(false) }
    var showConnectionsDialog by remember { mutableStateOf(false) }

    val green = Color(0xFF22C55E)

    if (person.isLinkedToUser) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = green.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, green.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Link, contentDescription = null, tint = green, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "מקושר לחשבון משתמש באפליקציה",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = green,
                        )
                    }
                    IconButton(onClick = { showUnlinkConfirmDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.LinkOff,
                            contentDescription = "בטל קישור",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "רשומה זו מקושרת ומאומתת לחשבון שורשים פעיל.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (actions.onGetMutualConnections != null) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showConnectionsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Outlined.Park, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("צפייה בפרופיל ובקשרים משותפים", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    } else {
        OutlinedButton(
            onClick = { showLinkDialog = true },
            modifier = modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
        ) {
            Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "קשר לחשבון משתמש בעץ", fontWeight = FontWeight.Bold)
        }
    }

    if (showLinkDialog) {
        LinkUserDialog(
            person = person,
            onSearchUser = actions.onSearchUser,
            onGetFriends = actions.onGetFriends,
            onConfirmLink = { targetUserId ->
                showLinkDialog = false
                actions.onLinkUser(targetUserId)
            },
            onDismiss = { showLinkDialog = false },
        )
    }

    if (showUnlinkConfirmDialog) {
        ShorashimConfirmDialog(
            onDismissRequest = { showUnlinkConfirmDialog = false },
            onConfirm = {
                showUnlinkConfirmDialog = false
                actions.onUnlinkUser()
            },
            title = "ביטול קישור לחשבון",
            message = "האם לבטל את הקישור בין ${person.fullName} לחשבון המשתמש? הרשומה בעץ תישמר, אך החיבור לחשבון יוסר.",
            confirmText = "בטל קישור",
            dismissText = "ביטול",
            icon = Icons.Outlined.LinkOff,
            isDestructive = true,
        )
    }

    val linkedId = person.linkedUserId
    val mutual = actions.onGetMutualConnections
    if (showConnectionsDialog && linkedId != null && mutual != null) {
        MutualConnectionsDialog(
            person = person,
            userId = linkedId,
            onGetMutualConnections = mutual,
            onDismiss = { showConnectionsDialog = false },
        )
    }
}

@Composable
internal fun LinkUserDialog(
    person: Person,
    onSearchUser: suspend (String) -> UserProfile?,
    onGetFriends: (suspend () -> List<FriendUser>)?,
    onConfirmLink: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var searchInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var foundUser by remember { mutableStateOf<UserProfile?>(null) }
    var friendsList by remember { mutableStateOf<List<FriendUser>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserProfile?>(null) }
    var showConfirmPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (onGetFriends != null) {
            friendsList = onGetFriends()
        }
    }

    ShorashimDialog(
        onDismissRequest = onDismiss,
        title = "קישור ${person.fullName} למשתמש",
        subtitle = "חיבור רשומת העץ לפרופיל משתמש פעיל",
        heroIcon = Icons.Outlined.Link,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "בחר משתמש מאומת מרשימת החברים שלך או חפש לפי שם משתמש מדויק (@username):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    label = { Text("חיפוש לפי שם משתמש (@username)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (searchInput.isNotBlank()) {
                                    isSearching = true
                                    scope.launch {
                                        val clean = searchInput.trim().removePrefix("@")
                                        foundUser = onSearchUser(clean)
                                        isSearching = false
                                    }
                                }
                            },
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.Search, contentDescription = "חפש")
                            }
                        }
                    },
                )

                if (foundUser != null) {
                    Spacer(Modifier.height(10.dp))
                    Text("נמצא משתמש:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    UserSelectionRow(
                        displayName = foundUser!!.displayName.ifBlank { foundUser!!.fullName },
                        username = foundUser!!.username,
                        avatarUrl = foundUser!!.avatarUrl,
                        isSelected = selectedUser?.id == foundUser!!.id,
                        onClick = { selectedUser = foundUser },
                    )
                }

                if (friendsList.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("או בחר מחברייך:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    friendsList.forEach { friend ->
                        UserSelectionRow(
                            displayName = friend.displayName,
                            username = friend.username,
                            avatarUrl = friend.avatarUrl,
                            isSelected = selectedUser?.id == friend.id,
                            onClick = {
                                selectedUser = UserProfile(
                                    id = friend.id,
                                    username = friend.username,
                                    fullName = friend.displayName,
                                    displayName = friend.displayName,
                                    avatarUrl = friend.avatarUrl,
                                )
                            },
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (selectedUser != null) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF22C55E).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF22C55E)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "נבחר לקשר: ${selectedUser!!.displayName.ifBlank { selectedUser!!.fullName }} (@${selectedUser!!.username ?: ""})",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUser != null) {
                        showConfirmPrompt = true
                    }
                },
                enabled = selectedUser != null,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("המשך לקישור", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            ) {
                Text("ביטול")
            }
        },
    )

    if (showConfirmPrompt && selectedUser != null) {
        ShorashimConfirmDialog(
            onDismissRequest = { showConfirmPrompt = false },
            onConfirm = {
                showConfirmPrompt = false
                onConfirmLink(selectedUser!!.id)
            },
            title = "אישור קישור זהות",
            message = "האם לקשר את רשומת '${person.fullName}' בעץ לחשבון המשתמש של '${selectedUser!!.displayName}'?\nפעולה זו תחבר בין הרשומה לחשבון האמיתי באפליקציה.",
            confirmText = "אשר וחבר",
            dismissText = "ביטול",
            icon = Icons.Outlined.Link,
            isDestructive = false,
        )
    }
}

@Composable
internal fun MutualConnectionsDialog(
    person: Person,
    userId: String,
    onGetMutualConnections: suspend (String) -> List<MutualFamilyConnection>,
    onDismiss: () -> Unit,
) {
    var connections by remember { mutableStateOf<List<MutualFamilyConnection>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        isLoading = true
        connections = onGetMutualConnections(userId)
        isLoading = false
    }

    ShorashimDialog(
        onDismissRequest = onDismiss,
        title = "קשרים משפחתיים משותפים",
        subtitle = "עצי משפחה שבהם המשתמש משתתף או מקושר",
        heroIcon = Icons.Outlined.Park,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "עצי משפחה גלויים שבהם המשתמש המקושר משתתף או מקושר:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                } else if (connections.isNullOrEmpty()) {
                    Text(
                        text = "לא נמצאו עצי משפחה משותפים נוספים ששניכם מורשים לראות (או שהעצים הוגדרו כפרטיים).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    connections!!.forEach { conn ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Park,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "עץ: ${conn.familyName}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    )
                                    Text(
                                        text = "מופיע בתור: ${conn.personName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
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
                Text("הבנתי, תודה")
            }
        },
    )
}

@Composable
internal fun UserSelectionRow(
    displayName: String,
    username: String?,
    avatarUrl: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF22C55E).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF22C55E) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersonAvatar(
                photoPath = avatarUrl,
                firstName = displayName,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                )
                if (!username.isNullOrBlank()) {
                    Text(
                        text = "\u200E@$username",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

