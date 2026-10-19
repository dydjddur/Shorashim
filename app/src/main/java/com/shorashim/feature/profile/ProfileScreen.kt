package com.shorashim.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.automirrored.outlined.Logout
import com.shorashim.core.ui.component.ShorashimConfirmDialog
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.ui.component.ImageUtils
import com.shorashim.core.ui.component.PersonAvatar
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.Spacing
import com.shorashim.feature.settings.EditProfileDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    onNavigateToTree: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadData(isSilent = true)
    }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val msg = uiState.successMessage ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    ProfileScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onUpdateProfile = viewModel::updateProfile,
        onUpdateAvatar = viewModel::updateAvatar,
        onTogglePrivateAccount = viewModel::togglePrivateAccountMode,
        onLogout = onLogout,
        modifier = modifier,
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onUpdateProfile: (fullName: String, displayName: String, bio: String?) -> Unit,
    onUpdateAvatar: (avatarBase64: String?) -> Unit,
    onTogglePrivateAccount: (Boolean) -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val user = uiState.profile

    if (showLogoutDialog) {
        ShorashimConfirmDialog(
            onDismissRequest = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            title = stringResource(R.string.settings_logout_confirm_title),
            message = stringResource(R.string.settings_logout_confirm_message),
            confirmText = stringResource(R.string.settings_logout_short),
            dismissText = "הישאר מחובר",
            icon = Icons.AutoMirrored.Outlined.Logout,
            isDestructive = true,
        )
    }

    if (showEditProfileDialog && user != null) {
        EditProfileDialog(
            initialFullName = user.fullName,
            initialDisplayName = user.displayName,
            isSaving = uiState.isSaving,
            errorMessage = uiState.errorMessage,
            onDismiss = { showEditProfileDialog = false },
            onSave = { fullName, displayName ->
                onUpdateProfile(fullName, displayName, user.bio)
                showEditProfileDialog = false
            },
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingPhoto = true
            scope.launch(Dispatchers.IO) {
                val base64 = ImageUtils.uriToCompressedBase64(context, uri)
                withContext(Dispatchers.Main) {
                    isUploadingPhoto = false
                    if (base64 != null) {
                        onUpdateAvatar(base64)
                    }
                }
            }
        }
    }

    var fullNameInput by remember(user?.fullName) { mutableStateOf(user?.fullName.orEmpty()) }
    var displayNameInput by remember(user?.displayName) { mutableStateOf(user?.displayName.orEmpty()) }
    var bioInput by remember(user?.bio) { mutableStateOf(user?.bio.orEmpty()) }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading && user == null) {
            ShorashimLoadingState(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // Header / Avatar Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Avatar with camera edit icon
                        Box(contentAlignment = Alignment.BottomEnd) {
                            PersonAvatar(
                                photoPath = user?.avatarUrl,
                                firstName = user?.displayName?.ifBlank { user.fullName } ?: "משתמש",
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape),
                            )

                            if (isUploadingPhoto) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp,
                                        color = Color.White,
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                tonalElevation = 4.dp,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.CameraAlt,
                                        contentDescription = "החלף תמונת פרופיל",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = user?.displayName?.ifBlank { user.fullName } ?: "משתמש",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (!user?.username.isNullOrBlank()) {
                            Text(
                                text = "\u200E@${user?.username}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        if (!user?.email.isNullOrBlank()) {
                            Text(
                                text = user?.email.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (user?.avatarUrl != null) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onUpdateAvatar(null) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "הסר תמונה",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            ProfileStatItem(
                                title = "עצים",
                                value = uiState.families.size.toString(),
                            )
                            ProfileStatItem(
                                title = "נפשות",
                                value = uiState.families.sumOf { it.personCount }.toString(),
                            )
                            ProfileStatItem(
                                title = "חברים",
                                value = uiState.friendsCount.toString(),
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Action Buttons: עריכה & התנתקות
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Button(
                                onClick = { showEditProfileDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(Spacing.xs))
                                Text("עריכה", maxLines = 1, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showLogoutDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Spacer(Modifier.width(Spacing.xs))
                                Text(
                                    "התנתקות",
                                    maxLines = 1,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }

                // 2. מצב פרטי (Private Account Mode Switch)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (user?.isPrivate == true)
                            Color(0xFF22C55E).copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surface,
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (user?.isPrivate == true) Color(0xFF22C55E) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (user?.isPrivate == true) Color(0xFF22C55E).copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (user?.isPrivate == true) Icons.Outlined.Lock else Icons.Outlined.Security,
                                        contentDescription = null,
                                        tint = if (user?.isPrivate == true) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "מצב פרטי (חשבון סגור)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    )
                                    Text(
                                        text = if (user?.isPrivate == true) "מופעל • העצים והחברים מוסתרים" else "כבוי • פרופיל פתוח לחברים",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (user?.isPrivate == true) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Switch(
                                checked = user?.isPrivate == true,
                                onCheckedChange = onTogglePrivateAccount,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF22C55E),
                                    checkedBorderColor = Color(0xFF22C55E),
                                    uncheckedThumbColor = Color(0xFFF1F5F9),
                                    uncheckedTrackColor = Color(0xFF334155),
                                    uncheckedBorderColor = Color(0xFF475569),
                                ),
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "מצב פרטי אומר שזה מסתיר את עצי המשפחה שלך (גם אם הוגדרו כציבוריים) ואת רשימת החברים שלך ממשתמשים אחרים באפליקציה (בדומה לאינסטגרם).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                        )
                    }
                }

                // 3. עריכת פרטי משתמש (Edit Details Form)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "פרטים אישיים",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = fullNameInput,
                            onValueChange = { fullNameInput = it },
                            label = { Text("שם מלא") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = displayNameInput,
                            onValueChange = { displayNameInput = it },
                            label = { Text("כינוי לתצוגה") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = bioInput,
                            onValueChange = { bioInput = it },
                            label = { Text("אודותי (תיאור קצר)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = { onUpdateProfile(fullNameInput, displayNameInput, bioInput.ifBlank { null }) },
                            enabled = !uiState.isSaving && fullNameInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("שמור שינויים", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xl))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun ProfileStatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
