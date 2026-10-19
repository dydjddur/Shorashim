package com.shorashim.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Shared dialog for Shorashim: RTL, flat paper surface with a hairline border, an optional icon tile or avatar
 * anchor, serif title and full-width actions. The body is weighted inside [maxHeight] so the actions stay visible.
 */
@Composable
fun ShorashimDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    heroIcon: ImageVector? = null,
    iconTint: Color? = null,
    iconBackground: Color? = null,
    avatar: (@Composable () -> Unit)? = null,
    title: String? = null,
    subtitle: String? = null,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    maxHeight: Dp = 560.dp,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 440.dp)
                    .heightIn(max = maxHeight)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header: Hero Icon or Avatar
                    if (avatar != null) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            avatar()
                        }
                    } else if (heroIcon != null || icon != null) {
                        val displayIcon = heroIcon ?: icon!!
                        IconTile(
                            icon = displayIcon,
                            tint = iconTint ?: MaterialTheme.colorScheme.primary,
                            container = iconBackground ?: MaterialTheme.colorScheme.primaryContainer,
                            size = 56.dp,
                            iconSize = 28.dp,
                            cornerRadius = 18.dp,
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    // Title
                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Subtitle
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Content Body
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                    ) {
                        content()
                    }

                    // Action Buttons Footer
                    if (confirmButton != null || dismissButton != null) {
                        Spacer(Modifier.height(20.dp))
                        if (confirmButton != null && dismissButton != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    confirmButton()
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    dismissButton()
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                (confirmButton ?: dismissButton)?.invoke()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom confirmation dialog for deletions, logout, and unlinking.
 */
@Composable
fun ShorashimConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "אישור",
    dismissText: String = "ביטול",
    icon: ImageVector = Icons.Outlined.WarningAmber,
    isDestructive: Boolean = false,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary

    ShorashimDialog(
        onDismissRequest = onDismissRequest,
        icon = icon,
        iconTint = if (isDestructive) errorColor else primaryColor,
        iconBackground = if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        title = title,
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) errorColor else primaryColor,
                    contentColor = if (isDestructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = confirmText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(
                    text = dismissText,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                )
            }
        },
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )
    }
}

/**
 * Custom RTL-enforcing SnackbarHost with rounded corners and Shorashim styling.
 */
@Composable
fun ShorashimSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SnackbarHost(
            hostState = hostState,
            modifier = modifier,
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                actionColor = MaterialTheme.colorScheme.inversePrimary,
                dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}
