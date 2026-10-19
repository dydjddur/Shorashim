package com.shorashim.feature.friends

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
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
import com.shorashim.core.ui.component.ShorashimSnackbarHost
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import com.shorashim.core.ui.component.ShorashimConfirmDialog
import com.shorashim.core.ui.component.ShorashimDialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.core.model.Family
import com.shorashim.core.model.FriendRequest
import com.shorashim.core.model.FriendSuggestion
import com.shorashim.core.model.FriendUser
import com.shorashim.core.ui.component.PersonAvatar
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.Spacing
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.shorashim.feature.chat.ChatListRoute
import com.shorashim.feature.chat.ChatListViewModel

@Composable
fun FriendsRoute(
    viewModel: FriendsViewModel,
    chatListViewModel: ChatListViewModel? = null,
    onNavigateToTree: (String) -> Unit = {},
    onNavigateToChat: (otherUserId: String, otherUserName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadData(isSilent = true)
    }

    LaunchedEffect(uiState.actionMessage, uiState.errorMessage) {
        val msg = uiState.actionMessage ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    FriendsScreen(
        uiState = uiState,
        chatListViewModel = chatListViewModel,
        snackbarHostState = snackbarHostState,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onSendRequest = viewModel::sendFriendRequest,
        onRespondRequest = viewModel::respondToRequest,
        onViewFriendTrees = viewModel::viewFriendTrees,
        onDismissFriendTrees = viewModel::dismissFriendTrees,
        onRemoveFriend = viewModel::removeFriend,
        onOpenTree = onNavigateToTree,
        onOpenChat = onNavigateToChat,
        modifier = modifier,
    )
}

@Composable
fun FriendsScreen(
    uiState: FriendsUiState,
    chatListViewModel: ChatListViewModel? = null,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String) -> Unit,
    onSendRequest: (String) -> Unit,
    onRespondRequest: (requestId: String, accept: Boolean) -> Unit,
    onViewFriendTrees: (FriendUser) -> Unit,
    onDismissFriendTrees: () -> Unit,
    onRemoveFriend: (String) -> Unit,
    onOpenTree: (String) -> Unit,
    onOpenChat: (otherUserId: String, otherUserName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var friendPendingRemove by remember { mutableStateOf<FriendUser?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Friends / Add friends / Chats switch
        com.shorashim.core.ui.component.PillToggle(
            options = listOf(
                com.shorashim.core.ui.component.PillOption(
                    if (uiState.friends.isEmpty()) "חברים" else "חברים · ${uiState.friends.size}",
                    Icons.Outlined.People,
                ),
                com.shorashim.core.ui.component.PillOption("הוספה", Icons.Outlined.PersonAdd),
                com.shorashim.core.ui.component.PillOption("צ'אטים", Icons.AutoMirrored.Outlined.Chat),
            ),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> {
                    // Tab 0: Friends list & Pending requests
                    if (uiState.isLoading && uiState.friends.isEmpty() && uiState.pendingRequests.isEmpty()) {
                        ShorashimLoadingState(modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            // 1. Pending Requests (if any)
                            if (uiState.pendingRequests.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "בקשות חברות ממתינות (${uiState.pendingRequests.size}):",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                items(uiState.pendingRequests, key = { "req_${it.id}" }) { req ->
                                    FriendRequestCard(
                                        request = req,
                                        onAccept = { onRespondRequest(req.id, true) },
                                        onDecline = { onRespondRequest(req.id, false) },
                                    )
                                }
                            }

                            // 2. My Friends Header
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "החברים שלי (${uiState.friends.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (uiState.friends.isNotEmpty()) {
                                        TextButton(onClick = { selectedTab = 1 }) {
                                            Icon(
                                                imageVector = Icons.Outlined.PersonAdd,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "הוסף חברים",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Friends List or Empty State
                            if (uiState.friends.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.People,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(34.dp),
                                                )
                                            }
                                            Spacer(Modifier.height(16.dp))
                                            Text(
                                                text = "אין עדיין חברים ברשימה",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = "התחבר לחברים ומשפחה כדי לצפות בעצי המשפחה שלהם ולשוחח בצ'אט.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                            )
                                            Spacer(Modifier.height(18.dp))
                                            Button(
                                                onClick = { selectedTab = 1 },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.PersonAdd,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "מעבר להוספת חברים",
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(uiState.friends, key = { "friend_${it.id}" }) { friend ->
                                    FriendItemCard(
                                        friend = friend,
                                        onViewTrees = { onViewFriendTrees(friend) },
                                        onOpenChat = { onOpenChat(friend.id, friend.displayName) },
                                        onRemove = { friendPendingRemove = friend },
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Tab 1: "הוספת חברים" - Add Friends (Search + App Community Users)
                    if (uiState.isLoading && uiState.suggestions.isEmpty() && uiState.searchResults.isEmpty()) {
                        ShorashimLoadingState(modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            // Search Bar for Users
                            item {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    placeholder = { Text("חיפוש משתמשים לפי שם משתמש @ או שם...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                    trailingIcon = {
                                        if (uiState.isSearching) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        } else if (uiState.searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { onSearchQueryChange("") }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "נקה חיפוש",
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                )
                            }

                            // If search returned results
                            if (uiState.searchResults.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "תוצאות חיפוש (${uiState.searchResults.size}):",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                items(uiState.searchResults, key = { "search_${it.id}" }) { user ->
                                    val isAlreadyFriend = uiState.friends.any { it.id == user.id }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                PersonAvatar(
                                                    photoPath = user.avatarUrl,
                                                    firstName = user.displayName,
                                                    modifier = Modifier.size(44.dp),
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = user.displayName,
                                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    if (!user.username.isNullOrBlank()) {
                                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                            Text(
                                                                text = "@${user.username.removePrefix("@")}",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            if (isAlreadyFriend) {
                                                OutlinedButton(
                                                    onClick = { onOpenChat(user.id, user.displayName) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Outlined.Chat,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("צ'אט", fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Button(
                                                    onClick = { onSendRequest(user.id) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.PersonAdd,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("הוסף חבר", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (uiState.searchQuery.isNotBlank() && !uiState.isSearching) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                text = "לא נמצאו משתמשים עבור \"${uiState.searchQuery}\"",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }

                            // Community / App users section
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "💡 משתמשים באפליקציית שורשים",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "אנשים הרשומים באפליקציה שתוכל להוסיף לרשימת החברים שלך או לשוחח איתם ישירות:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (uiState.suggestions.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                text = "אין משתמשים חדשים להוספה כרגע (כולם כבר חברים שלך!)",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(uiState.suggestions, key = { "sug_${it.id}" }) { suggestion ->
                                    FriendSuggestionCard(
                                        suggestion = suggestion,
                                        onAdd = { onSendRequest(suggestion.id) },
                                        onOpenChat = { onOpenChat(suggestion.id, suggestion.displayName) },
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Tab 2: "צ'אטים" - Chats list
                    if (chatListViewModel != null) {
                        ChatListRoute(
                            viewModel = chatListViewModel,
                            onOpenConversation = onOpenChat,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

        // Confirm Remove Friend Dialog
        friendPendingRemove?.let { victim ->
            ShorashimConfirmDialog(
                onDismissRequest = { friendPendingRemove = null },
                onConfirm = {
                    onRemoveFriend(victim.id)
                    friendPendingRemove = null
                },
                title = "הסרת חבר",
                message = "האם להסיר את ${victim.displayName} מרשימת החברים שלך? תוכלו לשלוח בקשת חברות מחדש בכל עת.",
                confirmText = "הסר חבר",
                dismissText = "ביטול",
                isDestructive = true,
            )
        }

        // Friend Trees Modal Dialog
        if (uiState.selectedFriendForTrees != null) {
            val friend = uiState.selectedFriendForTrees
            ShorashimDialog(
                onDismissRequest = onDismissFriendTrees,
                avatar = {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        PersonAvatar(
                            photoPath = friend.avatarUrl,
                            firstName = friend.displayName,
                            modifier = Modifier.size(54.dp),
                        )
                    }
                },
                title = "עצי המשפחה של ${friend.displayName}",
                subtitle = if (!friend.username.isNullOrBlank()) {
                    "\u200E@${friend.username.removePrefix("@")} • אילנות יוחסין גלויים"
                } else {
                    "אילנות יוחסין משפחתיים"
                },
                confirmButton = {
                    Button(
                        onClick = onDismissFriendTrees,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(
                            text = "סגירה",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                },
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (uiState.isLoadingFriendTrees) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "טוען עצי משפחה...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else if (uiState.isFriendProfilePrivate) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "חשבון פרטי ומוגן",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "משתמש זה בחר להגדיר את חשבונו כפרטי. עצי המשפחה ורשימת החברים שלו מוסתרים.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp,
                                    )
                                }
                            }
                        }
                    } else if (uiState.friendTrees.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Park,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "אין עצי משפחה גלויים",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "החבר טרם פרסם עצי משפחה לצפייה ציבורית באפליקציה.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "עצי משפחה לצפייה (${uiState.friendTrees.size}):",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        uiState.friendTrees.forEach { tree ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AccountTree,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = tree.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${tree.personCount} נפשות בעץ",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            onDismissFriendTrees()
                                            onOpenTree(tree.id)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    ) {
                                        Text(
                                            text = "צפה בעץ",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        ShorashimSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}
}

@Composable
private fun FriendItemCard(
    friend: FriendUser,
    onViewTrees: () -> Unit,
    onOpenChat: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Top Row: Avatar + Name & Username & Status + Remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar with online presence badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    PersonAvatar(
                        photoPath = friend.avatarUrl,
                        firstName = friend.displayName,
                        modifier = Modifier.size(50.dp),
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                color = if (friend.isOnline) com.shorashim.core.ui.theme.ShorashimTheme.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.surface), CircleShape)
                    )
                }

                Spacer(Modifier.width(14.dp))

                // Name, Username & Status column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = friend.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (!friend.username.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "@${friend.username.removePrefix("@")}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (friend.isOnline) com.shorashim.core.ui.theme.ShorashimTheme.colors.success else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (friend.isOnline) "מחובר עכשיו" else friend.lastSeenText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (friend.isOnline) com.shorashim.core.ui.theme.ShorashimTheme.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Remove friend icon button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "הסר חבר",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(Modifier.height(12.dp))

            // Action Buttons: Balanced full-width row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onViewTrees,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Park,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "עצי משפחה",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Button(
                    onClick = onOpenChat,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "צ'אט",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendSuggestionCard(
    suggestion: FriendSuggestion,
    onAdd: () -> Unit,
    onOpenChat: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                PersonAvatar(
                    photoPath = suggestion.avatarUrl,
                    firstName = suggestion.displayName,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!suggestion.username.isNullOrBlank()) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "@${suggestion.username.removePrefix("@")}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = suggestion.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onOpenChat,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("צ'אט", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("הוסף חבר", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun FriendRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PersonAvatar(
                    photoPath = request.fromAvatarUrl,
                    firstName = request.fromDisplayName,
                    modifier = Modifier.size(48.dp),
                )

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.fromDisplayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!request.fromUsername.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "@${request.fromUsername.removePrefix("@")}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "בקשת חברות • ${request.createdAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("דחה", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    Icon(imageVector = Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("אשר חברות", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
