package com.shorashim.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun LoginRoute(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.loginUiState.collectAsStateWithLifecycle()

    LoginScreen(
        uiState = uiState,
        onEmailChanged = viewModel::onLoginEmailChanged,
        onPasswordChanged = viewModel::onLoginPasswordChanged,
        onTogglePasswordVisibility = viewModel::toggleLoginPasswordVisibility,
        onLoginClicked = { viewModel.login(onLoginSuccess) },
        onNavigateToRegister = onNavigateToRegister,
        onNavigateToForgotPassword = onNavigateToForgotPassword,
        modifier = modifier,
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClicked: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    AuthScreenBackground(modifier = modifier) {
        // App Logo & Branding Header
        AuthHeader(
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.auth_subtitle),
        )

        Spacer(Modifier.height(Spacing.lg))

        // Elevated Form Card
        AuthCard {
            Text(
                text = stringResource(R.string.auth_login_title),
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

            // Username or Email Field
            AuthTextField(
                value = uiState.usernameOrEmail,
                onValueChange = onEmailChanged,
                label = stringResource(R.string.auth_username_or_email_label),
                placeholder = stringResource(R.string.auth_username_or_email_hint),
                leadingIcon = Icons.Outlined.Person,
                isError = uiState.usernameOrEmailError != null,
                errorMessage = uiState.usernameOrEmailError?.let { stringResource(it) },
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
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLoginClicked()
                    },
                ),
                enabled = !uiState.isLoading,
            )

            // Forgot Password link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    enabled = !uiState.isLoading,
                ) {
                    Text(
                        text = stringResource(R.string.auth_forgot_password_link),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Login Button
            AuthPrimaryButton(
                text = stringResource(R.string.auth_login_button),
                onClick = {
                    focusManager.clearFocus()
                    onLoginClicked()
                },
                isLoading = uiState.isLoading,
                enabled = !uiState.isLoading,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // Navigation to Register
        AuthFooterSwitcher(
            prefixText = "אין לך חשבון?",
            actionText = "הרשמה",
            onActionClick = onNavigateToRegister,
            enabled = !uiState.isLoading,
        )
    }
}
