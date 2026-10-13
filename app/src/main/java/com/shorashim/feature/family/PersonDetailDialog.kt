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
import androidx.compose.material3.AlertDialog
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

@Composable
fun PersonDetailDialog(
    person: Person,
    allPeople: List<Person>,
    relationships: List<Relationship>,
    canEdit: Boolean = false,
    onLinkUser: ((userId: String) -> Unit)? = null,
    onUnlinkUser: (() -> Unit)? = null,
    onSearchUser: (suspend (username: String) -> UserProfile?)? = null,
    onGetFriends: (suspend () -> List<FriendUser>)? = null,
    onGetMutualConnections: (suspend (userId: String) -> List<MutualFamilyConnection>)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val isDeceased = person.lifeStatus == LifeStatus.DECEASED
    val bDate = remember(person) { PartialDate.fromIso(person.birthDate, person.birthDatePrecision) }
    val dDate = remember(person) { PartialDate.fromIso(person.deathDate, person.deathDatePrecision) }

    var showLinkDialog by remember { mutableStateOf(false) }
    var showUnlinkConfirmDialog by remember { mutableStateOf(false) }
    var showConnectionsDialog by remember { mutableStateOf(false) }

    // Linked user info
    var linkedUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoadingLinkedProfile by remember { mutableStateOf(false) }

    LaunchedEffect(person.linkedUserId) {
        val linkedId = person.linkedUserId
        if (!linkedId.isNullOrBlank() && onSearchUser != null) {
            isLoadingLinkedProfile = true
            // Load profile if available
            val friends = onGetFriends?.invoke() ?: emptyList()
            val matchedFriend = friends.find { it.id == linkedId }
            if (matchedFriend != null) {
                linkedUserProfile = UserProfile(
                    id = matchedFriend.id,
                    username = matchedFriend.username,
                    fullName = matchedFriend.displayName,
                    displayName = matchedFriend.displayName,
                    avatarUrl = matchedFriend.avatarUrl,
                )
            }
            isLoadingLinkedProfile = false
        }
    }

    val bHebrew = remember(person.birthDate) {
        val iso = person.birthDate
        if (!iso.isNullOrBlank()) {
            runCatching {
                val parts = iso.split("-")
                if (parts.size == 3) {
                    val y = parts[0].toIntOrNull()
                    val m = parts[1].toIntOrNull()
                    val d = parts[2].toIntOrNull()
                    if (y != null && m != null && d != null) {
                        HebrewDates.formatHebrewDate(java.time.LocalDate.of(y, m, d))
                    } else null
                } else null
            }.getOrNull()
        } else null
    }

    val dHebrew = remember(person.deathDate) {
        val iso = person.deathDate
        if (!iso.isNullOrBlank()) {
            runCatching {
                val parts = iso.split("-")
                if (parts.size == 3) {
                    val y = parts[0].toIntOrNull()
                    val m = parts[1].toIntOrNull()
                    val d = parts[2].toIntOrNull()
                    if (y != null && m != null && d != null) {
                        HebrewDates.formatHebrewDate(java.time.LocalDate.of(y, m, d))
                    } else null
                } else null
            }.getOrNull()
        } else null
    }

    // Parents
    val parentIds = relationships
        .filter { it.relatedPersonId == person.id && it.relationshipType == RelationshipType.PARENT_CHILD }
        .map { it.personId }
    val parents = allPeople.filter { it.id in parentIds }

    // Spouses
    val spouseIds = relationships
        .filter { it.relationshipType == RelationshipType.SPOUSE && (it.personId == person.id || it.relatedPersonId == person.id) }
        .map { if (it.personId == person.id) it.relatedPersonId else it.personId }
    val spouses = allPeople.filter { it.id in spouseIds }

    // Children
    val childIds = relationships
        .filter { it.personId == person.id && it.relationshipType == RelationshipType.PARENT_CHILD }
        .map { it.relatedPersonId }
    val children = allPeople.filter { it.id in childIds }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(
                1.dp,
                if (isDeceased) Color(0xFFD4AF37).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                // Top row: Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "פרטי האדם",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "סגור",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Avatar + Full Name Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        PersonAvatar(
                            photoPath = person.photoPath,
                            firstName = person.firstName,
                            isDeceased = isDeceased,
                            size = 64.dp,
                            shape = CircleShape,
                        )
                        if (person.isLinkedToUser) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF22C55E),
                                modifier = Modifier.size(20.dp),
                                border = BorderStroke(1.5.dp, Color.White),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = "מקושר לחשבון",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            text = person.fullName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (!person.additionalNames.isNullOrBlank()) {
                            Text(
                                text = "כינוי: ${person.additionalNames}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDeceased) Color(0xFFD4AF37).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = if (isDeceased) "ז״ל 🕯️ נפטר/ה" else "חי/ה 🌿",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isDeceased) Color(0xFFD4AF37) else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // -------------------------------------------------------------
                // Feature 5: Linked User Account Card
                // -------------------------------------------------------------
                if (person.isLinkedToUser) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF22C55E).copy(alpha = 0.08f),
                        ),
                        border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f)),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "מקושר לחשבון משתמש באפליקציה",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF2E7D32),
                                    )
                                }

                                if (canEdit && onUnlinkUser != null) {
                                    IconButton(
                                        onClick = { showUnlinkConfirmDialog = true },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LinkOff,
                                            contentDescription = "בטל קישור",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "רשומה זו מקושרת ומאומתת לחשבון שורשים פעיל.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { showConnectionsDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Park,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("צפייה בפרופיל ובקשרים משותפים", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                } else if (canEdit && onLinkUser != null) {
                    OutlinedButton(
                        onClick = { showLinkDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "קשר לחשבון משתמש באפליקציה",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // Birth Date Info
                if (bDate.year != null || !person.birthDate.isNullOrBlank()) {
                    DetailRow(
                        icon = Icons.Outlined.Cake,
                        label = "תאריך לידה",
                        value = buildString {
                            append(bDate.toDisplayString())
                            if (!bHebrew.isNullOrBlank()) {
                                append(" ($bHebrew)")
                            }
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Death Date Info
                if (isDeceased && (dDate.year != null || !person.deathDate.isNullOrBlank())) {
                    DetailRow(
                        icon = Icons.Outlined.Info,
                        label = "תאריך פטירה",
                        value = buildString {
                            append(dDate.toDisplayString())
                            if (!dHebrew.isNullOrBlank()) {
                                append(" ($dHebrew)")
                            }
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Biography / Notes
                if (!person.biography.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "סיפור חיים / הערות",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = person.biography,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Family Relations Section
                if (parents.isNotEmpty() || spouses.isNotEmpty() || children.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "קשרים משפחתיים בעץ",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))

                    if (parents.isNotEmpty()) {
                        Text(
                            text = "הורים: ${parents.joinToString(", ") { it.fullName }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    if (spouses.isNotEmpty()) {
                        Text(
                            text = "בן/בת זוג: ${spouses.joinToString(", ") { it.fullName }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    if (children.isNotEmpty()) {
                        Text(
                            text = "ילדים: ${children.joinToString(", ") { it.fullName }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(text = "סגור")
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Sub-Dialog 1: Link Person to User Account
    // -------------------------------------------------------------------------
    if (showLinkDialog && onSearchUser != null && onLinkUser != null) {
        LinkUserDialog(
            person = person,
            onSearchUser = onSearchUser,
            onGetFriends = onGetFriends,
            onConfirmLink = { targetUserId ->
                showLinkDialog = false
                onLinkUser(targetUserId)
            },
            onDismiss = { showLinkDialog = false },
        )
    }

    // -------------------------------------------------------------------------
    // Sub-Dialog 2: Confirm Unlink Dialog
    // -------------------------------------------------------------------------
    if (showUnlinkConfirmDialog && onUnlinkUser != null) {
        AlertDialog(
            onDismissRequest = { showUnlinkConfirmDialog = false },
            title = { Text("ביטול קישור לחשבון") },
            text = {
                Text("האם אתה בטוח שברצונך לבטל את הקישור בין ${person.fullName} לחשבון המשתמש? הרשומה בעץ תישמר, אך החיבור לחשבון יוסר.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnlinkConfirmDialog = false
                        onUnlinkUser()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("בטל קישור", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkConfirmDialog = false }) {
                    Text("ביטול")
                }
            },
        )
    }

    // -------------------------------------------------------------------------
    // Sub-Dialog 3: View Mutual Family Connections & Profile
    // -------------------------------------------------------------------------
    if (showConnectionsDialog && person.linkedUserId != null && onGetMutualConnections != null) {
        MutualConnectionsDialog(
            person = person,
            userId = person.linkedUserId,
            onGetMutualConnections = onGetMutualConnections,
            onDismiss = { showConnectionsDialog = false },
        )
    }
}

@Composable
private fun LinkUserDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("קישור ${person.fullName} למשתמש") },
        text = {
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
            ) {
                Text("המשך לקישור", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ביטול")
            }
        },
    )

    if (showConfirmPrompt && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showConfirmPrompt = false },
            title = { Text("אישור קישור זהות") },
            text = {
                Text(
                    text = "האם אתה מאשר לקשר את רשומת '${person.fullName}' בעץ לחשבון המשתמש של '${selectedUser!!.displayName}'?\n\nפעולה זו תחבר בין הרשומה לחשבון האמיתי באפליקציה.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmPrompt = false
                        onConfirmLink(selectedUser!!.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                ) {
                    Text("אשר וחבר", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPrompt = false }) {
                    Text("ביטול")
                }
            },
        )
    }
}

@Composable
private fun MutualConnectionsDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("קשרים משפחתיים משותפים")
        },
        text = {
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
            Button(onClick = onDismiss) {
                Text("סגור")
            }
        },
    )
}

@Composable
private fun UserSelectionRow(
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

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
