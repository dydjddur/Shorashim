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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AlertDialog
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
            Column(modifier = Modifier.fillMaxSize()) {
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
        tonalElevation = 2.dp,
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
                        modifier = Modifier.size(11.dp),
                        tint = Color(0xFF22C55E),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "מוצפן מקצה לקצה (E2EE)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = Color(0xFF22C55E),
                        ),
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
            val bubbleBg = if (isFromMe) {
                Color(0xFF1B382B) // Elegant dark emerald
            } else {
                Color(0xFF1E2623) // Sleek slate charcoal
            }

            val bubbleBorder = if (isFromMe) {
                BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f))
            } else {
                BorderStroke(1.dp, Color(0xFF334155))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromMe) 16.dp else 4.dp,
                    bottomEnd = if (isFromMe) 4.dp else 16.dp,
                ),
                color = bubbleBg,
                border = bubbleBorder,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = message.plaintext,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            lineHeight = 20.sp,
                        ),
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.align(if (isFromMe) Alignment.End else Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatTime(message.createdAt),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                            ),
                        )
                        if (isFromMe) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = if (message.isRead) Icons.Outlined.DoneAll else Icons.Outlined.Check,
                                contentDescription = null,
                                tint = if (message.isRead) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp),
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Invite Tree button
            IconButton(
                onClick = onOpenTreePicker,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Park,
                    contentDescription = "הזמן לעץ משפחה",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(6.dp))

            // Input field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = {
                    Text(
                        text = "כתוב הודעה מוצפנת...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF22C55E),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                maxLines = 4,
            )

            Spacer(Modifier.width(6.dp))

            // Send button
            IconButton(
                onClick = onSendMessage,
                enabled = inputText.isNotBlank() && !isSending,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (inputText.isNotBlank() && !isSending) Color(0xFF22C55E) else Color(0xFF2E3832),
                        shape = CircleShape,
                    ),
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "שלח",
                        tint = if (inputText.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp),
                    )
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
                    tint = Color(0xFF22C55E),
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
                                text = "שלח הזמנה ✉️",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF22C55E),
                                    fontWeight = FontWeight.Bold,
                                ),
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
        val instant = java.time.Instant.parse(isoString)
        val zdt = instant.atZone(java.time.ZoneId.systemDefault())
        java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(zdt)
    } catch (e: Throwable) {
        ""
    }
}
