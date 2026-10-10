package com.shorashim.feature.settings

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.shorashim.R
import com.shorashim.core.model.AuthState
import com.shorashim.core.ui.component.Spacing

@Composable
fun SettingsRoute(
    authState: AuthState = AuthState.Unauthenticated,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    } catch (_: PackageManager.NameNotFoundException) {
        "1.0"
    }
    SettingsScreen(
        versionName = versionName,
        authState = authState,
        onLogout = onLogout,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    versionName: String,
    authState: AuthState = AuthState.Unauthenticated,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_logout_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_logout_confirm_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        // Account Card
        AccountCard(
            authState = authState,
            onLogoutClicked = { showLogoutDialog = true },
        )

        SettingsInfoCard(
            title = stringResource(R.string.settings_about_title),
            body = stringResource(R.string.settings_about_body),
        )
        SettingsInfoCard(
            title = stringResource(R.string.settings_privacy_title),
            body = stringResource(R.string.settings_privacy_body),
        )
        SettingsInfoCard(
            title = stringResource(R.string.settings_notifications_title),
            body = stringResource(R.string.settings_notifications_body),
        )
        Text(
            text = stringResource(R.string.settings_language_label) + ": " +
                stringResource(R.string.settings_language_value),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.settings_version_label) + ": " + versionName,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun AccountCard(
    authState: AuthState,
    onLogoutClicked: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = stringResource(R.string.settings_auth_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.sm))

            when (authState) {
                is AuthState.Authenticated -> {
                    if (authState.user.displayName.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.settings_account_name) + " " + authState.user.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                    }
                    Text(
                        text = stringResource(R.string.settings_account_email) + " " + authState.user.email,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.settings_account_status) + " " +
                            stringResource(R.string.settings_account_status_active),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    OutlinedButton(
                        onClick = onLogoutClicked,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(stringResource(R.string.settings_logout_button))
                    }
                }
                is AuthState.Unconfigured -> {
                    Text(
                        text = authState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.settings_auth_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoCard(
    title: String,
    body: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
