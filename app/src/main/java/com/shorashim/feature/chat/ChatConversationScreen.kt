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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.core.model.DecryptedChatMessage
import com.shorashim.core.model.Family
import com.shorashim.core.model.MessageType
import com.shorashim.core.ui.component.PersonAvatar
import com.shorashim.core.ui.component.ShorashimLoadingState

@Composable
fun ChatConversationRoute(
    viewModel: ChatConversationViewModel,
    onNavigateBack: () -> Unit,
    onOpenInvitation: (token: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.toastMessage, uiState.error) {
        val msg = uiState.toastMessage ?: uiState.error
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    ChatConversationScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onInputTextChanged = viewModel::onInputTextChanged,
        onSendMessage = viewModel::sendMessage,
        onOpenTreePicker = viewModel::openTreePicker,
        onDismissTreePicker = viewModel::dismissTreePicker,
        onSelectFamilyToInvite = viewModel::sendFamilyInvitation,
        onOpenInvitation = onOpenInvitation,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    uiState: ChatConversationUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onOpenTreePicker: () -> Unit,
    onDismissTreePicker: () -> Unit,
    onSelectFamilyToInvite: (Family) -> Unit,
    onOpenInvitation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Scroll to bottom when messages update
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                // Top Bar
                ChatTopBar(
                    userName = uiState.otherUserName.ifBlank { "שיחה פרטית" },
                    userAvatar = uiState.otherUserAvatar,
                    onNavigateBack = onNavigateBack,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Messages List
                if (uiState.isLoading && uiState.messages.isEmpty()) {
                    ShorashimLoadingState(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (uiState.messages.isEmpty()) {
                            item(key = "empty") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    com.shorashim.core.ui.component.IconTile(
                                        icon = Icons.Outlined.Lock,
                                        size = 56.dp,
                                        iconSize = 26.dp,
                                        cornerRadius = 18.dp,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "תחילת השיחה",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "ההודעות מוצפנות מקצה לקצה. רק אתם והצד השני יכולים לקרוא אותן.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                }
                            }
                        }
                        items(uiState.messages, key = { it.id }) { msg ->
                            ChatMessageItem(
                                message = msg,
                                onOpenInvitation = onOpenInvitation,
                            )
                        }
                    }
                }

                // Bottom Composer
                ChatInputBar(
                    inputText = uiState.inputText,
                    isSending = uiState.isSending,
                    onInputTextChanged = onInputTextChanged,
                    onSendMessage = onSendMessage,
                    onOpenTreePicker = onOpenTreePicker,
                )
            }

            // Tree Picker Bottom Sheet
            if (uiState.isTreePickerOpen) {
                TreePickerBottomSheet(
                    myFamilies = uiState.myFamilies,
                    onSelectFamily = onSelectFamilyToInvite,
                    onDismiss = onDismissTreePicker,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 75.dp),
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    userName: String,
    userAvatar: String?,
    onNavigateBack: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "חזרה",
                )
            }

            PersonAvatar(
                photoPath = userAvatar,
                firstName = userName,
                modifier = Modifier.size(40.dp),
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = com.shorashim.core.ui.theme.ShorashimTheme.colors.success,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "מוצפן מקצה לקצה",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.shorashim.core.ui.theme.ShorashimTheme.colors.success,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: DecryptedChatMessage,
    onOpenInvitation: (String) -> Unit,
) {
    val isFromMe = message.isFromMe

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
    ) {
        if (message.messageType == MessageType.INVITATION && message.invitationPayload != null) {
            Box(modifier = Modifier.fillMaxWidth(0.9f)) {
                ChatInvitationCard(
                    payload = message.invitationPayload,
                    isFromMe = isFromMe,
                    onOpenInvitation = onOpenInvitation,
                )
            }
        } else {
            // Text message bubble
            val extra = com.shorashim.core.ui.theme.ShorashimTheme.colors
            val bubbleBg = if (isFromMe) extra.bubbleMine else extra.bubbleTheirs
            val onBubble = if (isFromMe) extra.onBubbleMine else extra.onBubbleTheirs

            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isFromMe) 20.dp else 6.dp,
                    bottomEnd = if (isFromMe) 6.dp else 20.dp,
                ),
                color = bubbleBg,
                border = if (isFromMe) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (isFromMe) 0.dp else 0.5.dp,
                modifier = Modifier
                    .padding(vertical = 1.dp)
                    .widthIn(max = 300.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = message.plaintext,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onBubble,
                    )

                    Spacer(Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatTime(message.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = onBubble.copy(alpha = 0.65f),
                        )
                        if (isFromMe) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = if (message.isRead) Icons.Outlined.DoneAll else Icons.Outlined.Check,
                                contentDescription = if (message.isRead) "נקרא" else "נשלח",
                                tint = if (message.isRead) MaterialTheme.colorScheme.inversePrimary else onBubble.copy(alpha = 0.65f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isSending: Boolean,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onOpenTreePicker: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val canSend = inputText.isNotBlank() && !isSending
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surface,
    ) {
        Column {
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                IconButton(onClick = onOpenTreePicker) {
                    Icon(
                        imageVector = Icons.Outlined.Park,
                        contentDescription = "הזמן לעץ משפחה",
                        tint = scheme.primary,
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChanged,
                    placeholder = {
                        Text(
                            text = "כתבו הודעה...",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = scheme.surfaceVariant,
                        unfocusedContainerColor = scheme.surfaceVariant,
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                )

                Spacer(Modifier.width(6.dp))

                IconButton(
                    onClick = onSendMessage,
                    enabled = canSend,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .size(48.dp)
                        .background(
                            color = if (canSend) scheme.primary else scheme.surfaceVariant,
                            shape = CircleShape,
                        ),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = scheme.primary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "שלח",
                            tint = if (canSend) scheme.onPrimary else scheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TreePickerBottomSheet(
    myFamilies: List<Family>,
    onSelectFamily: (Family) -> Unit,
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
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "בחר עץ משפחה להזמנה",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "יישלח קישור מאובטח ומוצפן בשיחה",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (myFamilies.isEmpty()) {
                Text(
                    text = "אין לך עדיין עצי משפחה לשיתוף",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                myFamilies.forEach { family ->
                    Surface(
                        onClick = { onSelectFamily(family) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = family.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = "${family.personCount} אנשים",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "שליחת הזמנה",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun formatTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return ""
    return try {
        val instant = com.shorashim.core.common.TimeFormat.parseInstant(isoString) ?: return ""
        val zdt = instant.atZone(java.time.ZoneId.systemDefault())
        java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(zdt)
    } catch (e: Throwable) {
        ""
    }
}
