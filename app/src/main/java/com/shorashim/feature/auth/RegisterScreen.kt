package com.shorashim.feature.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.MenuBook
import com.shorashim.core.ui.component.ShorashimDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.ui.component.Spacing

@Composable
fun RegisterRoute(
    viewModel: AuthViewModel,
    onNavigateBackToLogin: () -> Unit,
    onRegisterSuccess: (email: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.registerUiState.collectAsStateWithLifecycle()

    RegisterScreen(
        uiState = uiState,
        onNameChanged = viewModel::onRegisterNameChanged,
        onUsernameChanged = viewModel::onRegisterUsernameChanged,
        onEmailChanged = viewModel::onRegisterEmailChanged,
        onPasswordChanged = viewModel::onRegisterPasswordChanged,
        onConfirmPasswordChanged = viewModel::onRegisterConfirmPasswordChanged,
        onTogglePasswordVisibility = viewModel::toggleRegisterPasswordVisibility,
        onToggleConfirmPasswordVisibility = viewModel::toggleRegisterConfirmPasswordVisibility,
        onTermsAcceptedChanged = viewModel::onTermsAcceptedChanged,
        onOpenTermsDialog = { viewModel.setTermsDialogOpen(true) },
        onCloseTermsDialog = { viewModel.setTermsDialogOpen(false) },
        onRegisterClicked = {
            val email = uiState.email.trim()
            viewModel.register {
                onRegisterSuccess(email)
            }
        },
        onNavigateBackToLogin = onNavigateBackToLogin,
        modifier = modifier,
    )
}

@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    onNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit = {},
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onTermsAcceptedChanged: (Boolean) -> Unit,
    onOpenTermsDialog: () -> Unit,
    onCloseTermsDialog: () -> Unit,
    onRegisterClicked: () -> Unit,
    onNavigateBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    if (uiState.isTermsDialogOpen) {
        ShorashimDialog(
            onDismissRequest = onCloseTermsDialog,
            title = stringResource(R.string.auth_terms_dialog_title),
            subtitle = "הסכם שימוש ומדיניות פרטיות באפליקציה",
            heroIcon = Icons.Outlined.MenuBook,
            content = {
                Text(
                    text = stringResource(R.string.auth_terms_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            confirmButton = {
                Button(
                    onClick = onCloseTermsDialog,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.auth_close),
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
        )
    }

    AuthScreenBackground(modifier = modifier) {
        // App Branding Header
        AuthHeader(
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.auth_subtitle),
        )

        Spacer(Modifier.height(Spacing.lg))

        // Elevated Form Card
        AuthCard {
            Text(
                text = stringResource(R.string.auth_register_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = Spacing.md),
            )

            // General Error Banner
            if (uiState.generalError != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.md),
                ) {
                    Text(
                        text = stringResource(uiState.generalError),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = Modifier.padding(Spacing.md),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Full Name Field
            AuthTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                label = stringResource(R.string.auth_full_name_label),
                leadingIcon = Icons.Outlined.Person,
                isError = uiState.nameError != null,
                errorMessage = uiState.nameError?.let { stringResource(it) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                enabled = !uiState.isLoading,
            )

            Spacer(Modifier.height(Spacing.sm))

            // Username Field
            AuthTextField(
                value = uiState.username,
                onValueChange = onUsernameChanged,
                label = stringResource(R.string.auth_username_label),
                placeholder = stringResource(R.string.auth_username_hint),
                leadingIcon = Icons.Outlined.AlternateEmail,
                isError = uiState.usernameError != null,
                errorMessage = uiState.usernameError?.let { stringResource(it) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                enabled = !uiState.isLoading,
            )

            Spacer(Modifier.height(Spacing.sm))

            // Email Field (Optional for recovery)
            AuthTextField(
                value = uiState.email,
                onValueChange = onEmailChanged,
                label = stringResource(R.string.auth_email_optional_label),
                placeholder = stringResource(R.string.auth_email_hint),
                leadingIcon = Icons.Outlined.Email,
                isError = uiState.emailError != null,
                errorMessage = uiState.emailError?.let { stringResource(it) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                enabled = !uiState.isLoading,
            )

            Spacer(Modifier.height(Spacing.sm))

            // Password Field
            AuthTextField(
                value = uiState.password,
                onValueChange = onPasswordChanged,
                label = stringResource(R.string.auth_password_label),
                leadingIcon = Icons.Outlined.Lock,
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = stringResource(
                                if (uiState.isPasswordVisible) {
                                    R.string.auth_hide_password
                                } else {
                                    R.string.auth_show_password
                                },
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                visualTransformation = if (uiState.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                isError = uiState.passwordError != null,
                errorMessage = uiState.passwordError?.let { stringResource(it) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                enabled = !uiState.isLoading,
            )

            Spacer(Modifier.height(Spacing.sm))

            // Confirm Password Field
            AuthTextField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChanged,
                label = stringResource(R.string.auth_confirm_password_label),
                leadingIcon = Icons.Outlined.Lock,
                trailingIcon = {
                    IconButton(onClick = onToggleConfirmPasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isConfirmPasswordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = stringResource(
                                if (uiState.isConfirmPasswordVisible) {
                                    R.string.auth_hide_password
                                } else {
                                    R.string.auth_show_password
                                },
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                visualTransformation = if (uiState.isConfirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                isError = uiState.confirmPasswordError != null,
                errorMessage = uiState.confirmPasswordError?.let { stringResource(it) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onRegisterClicked()
                    },
                ),
                enabled = !uiState.isLoading,
            )

            Spacer(Modifier.height(Spacing.sm))

            // Terms Checkbox & Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !uiState.isLoading,
                        onClick = { onTermsAcceptedChanged(!uiState.termsAccepted) },
                    )
                    .padding(vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = uiState.termsAccepted,
                    onCheckedChange = onTermsAcceptedChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline,
                    ),
                    enabled = !uiState.isLoading,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.auth_terms_checkbox),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.auth_terms_link),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(
                                enabled = !uiState.isLoading,
                                onClick = onOpenTermsDialog,
                            )
                            .padding(top = 2.dp),
                    )
                }
            }

            if (uiState.termsError != null) {
                Text(
                    text = stringResource(uiState.termsError),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }

            Spacer(Modifier.height(Spacing.md))

            // Register Button
            AuthPrimaryButton(
                text = stringResource(R.string.auth_register_button),
                onClick = {
                    focusManager.clearFocus()
                    onRegisterClicked()
                },
                isLoading = uiState.isLoading,
                enabled = !uiState.isLoading,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // Back to Login Switcher
        AuthFooterSwitcher(
            prefixText = "כבר יש לך חשבון?",
            actionText = "התחברות",
            onActionClick = onNavigateBackToLogin,
            enabled = !uiState.isLoading,
        )
    }
}
