package com.shorashim.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.ui.component.Spacing

@Composable
fun ForgotPasswordRoute(
    viewModel: AuthViewModel,
    onNavigateBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.forgotPasswordUiState.collectAsStateWithLifecycle()

    ForgotPasswordScreen(
        uiState = uiState,
        onEmailChanged = viewModel::onForgotPasswordEmailChanged,
        onSendResetEmailClicked = viewModel::sendPasswordResetEmail,
        onNavigateBackToLogin = {
            viewModel.resetForgotPasswordState()
            onNavigateBackToLogin()
        },
        modifier = modifier,
    )
}

@Composable
fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onEmailChanged: (String) -> Unit,
    onSendResetEmailClicked: () -> Unit,
    onNavigateBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    AuthScreenBackground(modifier = modifier) {
        // App Branding Header
        AuthHeader(
            title = stringResource(R.string.auth_forgot_password_title),
            subtitle = stringResource(R.string.auth_forgot_password_description),
            icon = Icons.Outlined.LockReset,
        )

        Spacer(Modifier.height(Spacing.lg))

        // Elevated Form Card
        AuthCard {
            // Success Card
            if (uiState.isEmailSent) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.md),
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = stringResource(R.string.auth_reset_email_sent_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = stringResource(R.string.auth_reset_email_sent_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

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

            if (!uiState.isEmailSent) {
                // Email Field
                AuthTextField(
                    value = uiState.email,
                    onValueChange = onEmailChanged,
                    label = stringResource(R.string.auth_email_label),
                    placeholder = stringResource(R.string.auth_email_hint),
                    leadingIcon = Icons.Outlined.Email,
                    isError = uiState.emailError != null,
                    errorMessage = uiState.emailError?.let { stringResource(it) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onSendResetEmailClicked()
                        },
                    ),
                    enabled = !uiState.isLoading,
                )

                Spacer(Modifier.height(Spacing.md))

                // Submit Button
                AuthPrimaryButton(
                    text = stringResource(R.string.auth_send_reset_link_button),
                    onClick = {
                        focusManager.clearFocus()
                        onSendResetEmailClicked()
                    },
                    isLoading = uiState.isLoading,
                    enabled = !uiState.isLoading,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // Back to Login Switcher
        AuthFooterSwitcher(
            prefixText = "זוכר/ת את הסיסמה?",
            actionText = stringResource(R.string.auth_back_to_login),
            onActionClick = onNavigateBackToLogin,
            enabled = !uiState.isLoading,
        )
    }
}
