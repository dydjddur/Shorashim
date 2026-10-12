package com.shorashim.feature.auth

import androidx.annotation.StringRes

/**
 * UI states for authentication screens.
 */

enum class ForgotPasswordStep {
    ENTER_USERNAME,
    ENTER_CODE,
    ENTER_NEW_PASSWORD,
    SUCCESS,
}

data class LoginUiState(
    val usernameOrEmail: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    @StringRes val usernameOrEmailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val generalError: Int? = null,
)

data class RegisterUiState(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val termsAccepted: Boolean = false,
    val isTermsDialogOpen: Boolean = false,
    val isLoading: Boolean = false,
    @StringRes val nameError: Int? = null,
    @StringRes val usernameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmPasswordError: Int? = null,
    @StringRes val termsError: Int? = null,
    @StringRes val generalError: Int? = null,
)

data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.ENTER_USERNAME,
    val username: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isNewPasswordVisible: Boolean = false,
    val isConfirmNewPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val hasEmail: Boolean = true,
    val cooldownSeconds: Int = 0,
    @StringRes val usernameError: Int? = null,
    @StringRes val codeError: Int? = null,
    @StringRes val newPasswordError: Int? = null,
    @StringRes val confirmNewPasswordError: Int? = null,
    @StringRes val generalError: Int? = null,
)
