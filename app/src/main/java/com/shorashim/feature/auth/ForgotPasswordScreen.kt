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
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
        onUsernameChanged = viewModel::onForgotPasswordUsernameChanged,
        onCodeChanged = viewModel::onForgotPasswordCodeChanged,
        onNewPasswordChanged = viewModel::onForgotPasswordNewPasswordChanged,
        onConfirmNewPasswordChanged = viewModel::onForgotPasswordConfirmNewPasswordChanged,
        onToggleNewPasswordVisibility = viewModel::toggleNewPasswordVisibility,
        onToggleConfirmNewPasswordVisibility = viewModel::toggleConfirmNewPasswordVisibility,
        onRequestCodeClicked = viewModel::requestCode,
        onVerifyCodeClicked = viewModel::verifyCode,
        onCompleteResetClicked = { viewModel.completePasswordReset(onSuccess = {}) },
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
    onUsernameChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmNewPasswordChanged: (String) -> Unit,
    onToggleNewPasswordVisibility: () -> Unit,
    onToggleConfirmNewPasswordVisibility: () -> Unit,
    onRequestCodeClicked: () -> Unit,
    onVerifyCodeClicked: () -> Unit,
    onCompleteResetClicked: () -> Unit,
    onNavigateBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    AuthScreenBackground(modifier = modifier) {
        AuthHeader(
            title = stringResource(R.string.auth_forgot_password_title),
            subtitle = when (uiState.step) {
                ForgotPasswordStep.ENTER_USERNAME -> "הזינו את שם המשתמש שלכם לשליחת קוד אימות זמני למייל"
                ForgotPasswordStep.ENTER_CODE -> "הזינו את קוד האימות בן 6 הספרות שנשלח לכתובת המייל המאומתת שלכם"
                ForgotPasswordStep.ENTER_NEW_PASSWORD -> "הזינו את הסיסמה החדשה שלכם"
                ForgotPasswordStep.SUCCESS -> "הסיסמה עודכנה בהצלחה"
            },
            icon = Icons.Outlined.LockReset,
        )

        Spacer(Modifier.height(Spacing.lg))

        AuthCard {
            if (uiState.step == ForgotPasswordStep.SUCCESS) {
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
                            text = "הסיסמה עודכנה בהצלחה!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = "כעת ניתן להתחבר לאפליקציה באמצעות הסיסמה החדשה.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                AuthPrimaryButton(
                    text = stringResource(R.string.auth_back_to_login),
                    onClick = onNavigateBackToLogin,
                )
            } else {
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
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(Spacing.md),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                if (!uiState.hasEmail) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.md),
                    ) {
                        Text(
                            text = "לחשבון זה לא מוגדרת כתובת אימייל מאומתת לשחזור. לא ניתן לאפס סיסמה עצמאית, אנא פנו למנהל המערכת.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(Spacing.md),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                when (uiState.step) {
                    ForgotPasswordStep.ENTER_USERNAME -> {
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
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onRequestCodeClicked()
                                },
                            ),
                            enabled = !uiState.isLoading,
                        )

                        Spacer(Modifier.height(Spacing.md))

                        AuthPrimaryButton(
                            text = stringResource(R.string.auth_send_code_button),
                            onClick = {
                                focusManager.clearFocus()
                                onRequestCodeClicked()
                            },
                            isLoading = uiState.isLoading,
                            enabled = !uiState.isLoading,
                        )
                    }

                    ForgotPasswordStep.ENTER_CODE -> {
                        AuthTextField(
                            value = uiState.code,
                            onValueChange = onCodeChanged,
                            label = stringResource(R.string.auth_reset_code_label),
                            placeholder = "123456",
                            leadingIcon = Icons.Outlined.LockReset,
                            isError = uiState.codeError != null,
                            errorMessage = uiState.codeError?.let { stringResource(it) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onVerifyCodeClicked()
                                },
                            ),
                            enabled = !uiState.isLoading,
                        )

                        Spacer(Modifier.height(Spacing.md))

                        AuthPrimaryButton(
                            text = stringResource(R.string.auth_verify_code_button),
                            onClick = {
                                focusManager.clearFocus()
                                onVerifyCodeClicked()
                            },
                            isLoading = uiState.isLoading,
                            enabled = !uiState.isLoading,
                        )
                    }

                    ForgotPasswordStep.ENTER_NEW_PASSWORD -> {
                        AuthTextField(
                            value = uiState.newPassword,
                            onValueChange = onNewPasswordChanged,
                            label = stringResource(R.string.auth_new_password_label),
                            leadingIcon = Icons.Outlined.Lock,
                            trailingIcon = {
                                IconButton(onClick = onToggleNewPasswordVisibility) {
                                    Icon(
                                        imageVector = if (uiState.isNewPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            visualTransformation = if (uiState.isNewPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            isError = uiState.newPasswordError != null,
                            errorMessage = uiState.newPasswordError?.let { stringResource(it) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next,
                            ),
                            enabled = !uiState.isLoading,
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        AuthTextField(
                            value = uiState.confirmNewPassword,
                            onValueChange = onConfirmNewPasswordChanged,
                            label = stringResource(R.string.auth_new_password_confirm_label),
                            leadingIcon = Icons.Outlined.Lock,
                            trailingIcon = {
                                IconButton(onClick = onToggleConfirmNewPasswordVisibility) {
                                    Icon(
                                        imageVector = if (uiState.isConfirmNewPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            visualTransformation = if (uiState.isConfirmNewPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            isError = uiState.confirmNewPasswordError != null,
                            errorMessage = uiState.confirmNewPasswordError?.let { stringResource(it) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onCompleteResetClicked()
                                },
                            ),
                            enabled = !uiState.isLoading,
                        )

                        Spacer(Modifier.height(Spacing.md))

                        AuthPrimaryButton(
                            text = stringResource(R.string.auth_reset_password_button),
                            onClick = {
                                focusManager.clearFocus()
                                onCompleteResetClicked()
                            },
                            isLoading = uiState.isLoading,
                            enabled = !uiState.isLoading,
                        )
                    }

                    ForgotPasswordStep.SUCCESS -> Unit
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))

        AuthFooterSwitcher(
            prefixText = "זוכר/ת את הסיסמה?",
            actionText = stringResource(R.string.auth_back_to_login),
            onActionClick = onNavigateBackToLogin,
            enabled = !uiState.isLoading,
        )
    }
}
