package com.shorashim.feature.chat

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Lock
import com.shorashim.core.ui.component.ShorashimDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.core.model.Conversation
import com.shorashim.core.model.FriendUser
import com.shorashim.core.ui.component.PersonAvatar
import com.shorashim.core.ui.component.ShorashimLoadingState

@Composable
fun ChatListRoute(
    viewModel: ChatListViewModel,
    onOpenConversation: (otherUserId: String, otherUserName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadConversations(isSilent = true)
    }

    ChatListScreen(
        uiState = uiState,
        onOpenConversation = onOpenConversation,
        onOpenNewChat = viewModel::openNewChatDialog,
        onDismissNewChat = viewModel::dismissNewChatDialog,
        onSelectFriendToChat = { friend ->
            viewModel.dismissNewChatDialog()
            onOpenConversation(friend.id, friend.displayName)
        },
        modifier = modifier,
    )
}

@Composable
fun ChatListScreen(
    uiState: ChatListUiState,
    onOpenConversation: (otherUserId: String, otherUserName: String) -> Unit,
    onOpenNewChat: () -> Unit,
    onDismissNewChat: () -> Unit,
    onSelectFriendToChat: (FriendUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (uiState.isLoading && uiState.conversations.isEmpty()) {
                ShorashimLoadingState(modifier = Modifier.fillMaxSize())
            } else if (uiState.conversations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "אין שיחות פעילות",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "כל ההודעות וההזמנות מוצפנות מקצה לקצה (E2EE).\nלחץ על הכפתור למטה כדי להתחיל שיחה עם חבר!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onOpenNewChat,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddComment,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("התחל שיחה חדשה")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1B382B),
                            border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "השיחות מוצפנות מקצה לקצה. רק אתה והנמען יכולים לקרוא אותן.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1),
                                    ),
                                )
                            }
                        }
                    }

                    items(uiState.conversations, key = { it.id }) { conv ->
                        ConversationItemCard(
                            conversation = conv,
                            onClick = {
                                onOpenConversation(conv.otherUserId, conv.otherUserName)
                            },
                        )
                    }
                }
            }

            // Floating Action Button
            if (uiState.conversations.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onOpenNewChat,
                    containerColor = Color(0xFF22C55E),
                    contentColor = Color(0xFF0F172A),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddComment,
                        contentDescription = "שיחה חדשה",
                    )
                }
            }

            // New Chat Dialog (Pick from Friends)
            if (uiState.isNewChatDialogOpen) {
                ShorashimDialog(
                    onDismissRequest = onDismissNewChat,
                    icon = Icons.Outlined.Chat,
                    title = "שיחה חדשה ומאובטחת",
                    subtitle = "בחר חבר כדי להתחיל צ'אט פרטי מוצפן מקצה לקצה",
                    dismissButton = {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onDismissNewChat,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        ) {
                            Text(
                                text = "סגירה",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (uiState.friends.isEmpty() && uiState.appUsers.isEmpty()) {
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
                                    Text(
                                        text = "אין עדיין חברים זמינים לשיחה",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "הוסף חברים בלשונית 'הוספת חברים' כדי לשוחח איתם בהצפנה מלאה ופרטיות מוחלטת.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                }
                            }
                        } else if (uiState.friends.isEmpty()) {
                            Text(
                                text = "משתמשים זמינים באפליקציה:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(uiState.appUsers, key = { it.id }) { user ->
                                    Surface(
                                        onClick = {
                                            onSelectFriendToChat(
                                                FriendUser(
                                                    id = user.id,
                                                    username = user.username,
                                                    displayName = user.displayName,
                                                    avatarUrl = user.avatarUrl,
                                                )
                                            )
                                        },
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
                                        ) {
                                            PersonAvatar(
                                                photoPath = user.avatarUrl,
                                                firstName = user.displayName,
                                                modifier = Modifier.size(42.dp),
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = user.displayName,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                                if (!user.username.isNullOrBlank()) {
                                                    Text(
                                                        text = "\u200E@${user.username.removePrefix("@")}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "החברים שלך (${uiState.friends.size}):",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(uiState.friends, key = { it.id }) { friend ->
                                    Surface(
                                        onClick = { onSelectFriendToChat(friend) },
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
                                        ) {
                                            PersonAvatar(
                                                photoPath = friend.avatarUrl,
                                                firstName = friend.displayName,
                                                modifier = Modifier.size(42.dp),
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = friend.displayName,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                                if (!friend.username.isNullOrBlank()) {
                                                    Text(
                                                        text = "\u200E@${friend.username.removePrefix("@")}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItemCard(
    conversation: Conversation,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                PersonAvatar(
                    photoPath = conversation.otherUserAvatar,
                    firstName = conversation.otherUserName,
                    modifier = Modifier.size(48.dp),
                )
                if (conversation.isOtherUserOnline) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .background(Color(0xFF22C55E), CircleShape)
                            .clip(CircleShape),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = conversation.otherUserName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatChatDate(conversation.lastMessageAt),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = conversation.lastMessageText ?: "שיחה חדשה",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatChatDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return ""
    return try {
        val instant = java.time.Instant.parse(isoString)
        val zdt = instant.atZone(java.time.ZoneId.systemDefault())
        java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm").format(zdt)
    } catch (e: Throwable) {
        ""
    }
}
