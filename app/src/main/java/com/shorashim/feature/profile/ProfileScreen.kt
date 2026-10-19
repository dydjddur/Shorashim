package com.shorashim.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.ui.component.IconTile
import com.shorashim.core.ui.component.ImageUtils
import com.shorashim.core.ui.component.PersonAvatar
import com.shorashim.core.ui.component.SectionLabel
import com.shorashim.core.ui.component.ShorashimCard
import com.shorashim.core.ui.component.ShorashimConfirmDialog
import com.shorashim.core.ui.component.ShorashimErrorState
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.ShorashimSnackbarHost
import com.shorashim.core.ui.component.Spacing
import com.shorashim.core.ui.theme.ShorashimTheme
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
        // While there is no profile at all, the error is shown full-screen with a retry button instead.
        if (msg != null && uiState.profile != null) {
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
        onRetry = { viewModel.loadData() },
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
    onRetry: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }

    val user = uiState.profile
    val shownName = user?.displayName?.ifBlank { user.fullName } ?: "משתמש"

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
            scope.launch {
                // Decoding/compressing can throw on corrupt or unsupported images; never crash the screen for it.
                val base64 = withContext(Dispatchers.IO) {
                    runCatching { ImageUtils.uriToCompressedBase64(context, uri) }.getOrNull()
                }
                isUploadingPhoto = false
                if (base64 != null) {
                    onUpdateAvatar(base64)
                } else {
                    snackbarHostState.showSnackbar("לא הצלחנו לטעון את התמונה שנבחרה")
                }
            }
        }
    }
    val launchPicker: () -> Unit = {
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    var fullNameInput by rememberSaveable(user?.fullName) { mutableStateOf(user?.fullName.orEmpty()) }
    var displayNameInput by rememberSaveable(user?.displayName) { mutableStateOf(user?.displayName.orEmpty()) }
    var bioInput by rememberSaveable(user?.bio) { mutableStateOf(user?.bio.orEmpty()) }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && user == null -> ShorashimLoadingState(modifier = Modifier.fillMaxSize())
            user == null -> ShorashimErrorState(
                message = uiState.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // 1. Identity card
                ShorashimCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                PersonAvatar(
                                    photoPath = user.avatarUrl,
                                    firstName = shownName,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape),
                                )
                                if (isUploadingPhoto) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = launchPicker,
                                enabled = !isUploadingPhoto,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.CameraAlt,
                                        contentDescription = "החלף תמונת פרופיל",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(17.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = shownName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )

                        if (!user.username.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "‎@${user.username}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        if (!user.email.isNullOrBlank()) {
                            Text(
                                text = user.email.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (user.avatarUrl != null) {
                            TextButton(
                                onClick = { onUpdateAvatar(null) },
                                enabled = !isUploadingPhoto,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("הסר תמונה", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Stats strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ProfileStatItem(
                                title = "עצים",
                                value = uiState.families.size.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            StatDivider()
                            ProfileStatItem(
                                title = "נפשות",
                                value = uiState.families.sumOf { it.personCount }.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            StatDivider()
                            ProfileStatItem(
                                title = "חברים",
                                value = uiState.friendsCount.toString(),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Button(
                                onClick = { showEditProfileDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(Spacing.xs))
                                Text("עריכה", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { showLogoutDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(Spacing.xs))
                                Text("התנתקות", maxLines = 1)
                            }
                        }
                    }
                }

                // 2. Private account mode
                val isPrivate = user.isPrivate
                SectionLabel(text = "פרטיות")
                ShorashimCard(
                    onClick = { onTogglePrivateAccount(!isPrivate) },
                    containerColor = if (isPrivate) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    borderColor = if (isPrivate) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(icon = if (isPrivate) Icons.Outlined.Lock else Icons.Outlined.Security)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "מצב פרטי (חשבון סגור)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (isPrivate) "מופעל • העצים והחברים מוסתרים" else "כבוי • פרופיל פתוח לחברים",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPrivate) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isPrivate,
                            onCheckedChange = onTogglePrivateAccount,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "מצב פרטי מסתיר את עצי המשפחה שלך (גם אם הוגדרו כציבוריים) ואת רשימת החברים שלך ממשתמשים אחרים באפליקציה.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 3. Personal details form
                SectionLabel(text = "פרטים אישיים")
                ShorashimCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(
                            icon = Icons.Outlined.Person,
                            tint = ShorashimTheme.colors.gold,
                            container = ShorashimTheme.colors.goldContainer,
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "איך אחרים רואים אותך",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = fullNameInput,
                        onValueChange = { fullNameInput = it },
                        label = { Text("שם מלא") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        label = { Text("כינוי לתצוגה") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("אודותי (תיאור קצר)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        minLines = 2,
                        maxLines = 4,
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onUpdateProfile(fullNameInput.trim(), displayNameInput.trim(), bioInput.trim().ifBlank { null })
                        },
                        enabled = !uiState.isSaving && fullNameInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("שמור שינויים")
                    }
                }

                Spacer(Modifier.height(Spacing.xl))
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

@Composable
private fun StatDivider() {
    VerticalDivider(
        modifier = Modifier.height(28.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun ProfileStatItem(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
