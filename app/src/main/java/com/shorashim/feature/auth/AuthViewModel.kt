package com.shorashim.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.common.AuthErrorMapper
import com.shorashim.core.common.AuthValidator
import com.shorashim.core.model.AuthState
import com.shorashim.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing authentication state and user interactions for Login,
 * Registration, and Password Reset screens.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _registerUiState = MutableStateFlow(RegisterUiState())
    val registerUiState: StateFlow<RegisterUiState> = _registerUiState.asStateFlow()

    private val _forgotPasswordUiState = MutableStateFlow(ForgotPasswordUiState())
    val forgotPasswordUiState: StateFlow<ForgotPasswordUiState> = _forgotPasswordUiState.asStateFlow()

    // --- Login Handlers ---

    fun onLoginIdentifierChanged(identifier: String) {
        _loginUiState.update { it.copy(usernameOrEmail = identifier, usernameOrEmailError = null, generalError = null) }
    }

    fun onLoginEmailChanged(email: String) = onLoginIdentifierChanged(email)

    fun onLoginPasswordChanged(password: String) {
        _loginUiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun toggleLoginPasswordVisibility() {
        _loginUiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login(onSuccess: () -> Unit = {}) {
        val state = _loginUiState.value
        if (state.isLoading) return

        val identifierError = AuthValidator.validateLoginIdentifier(state.usernameOrEmail)
        val passwordError = AuthValidator.validatePassword(state.password)

        if (identifierError != null || passwordError != null) {
            _loginUiState.update {
                it.copy(
                    usernameOrEmailError = identifierError,
                    passwordError = passwordError,
                )
            }
            return
        }

        _loginUiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            val result = authRepository.login(state.usernameOrEmail, state.password)
            result.fold(
                onSuccess = {
                    _loginUiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { error ->
                    android.util.Log.e("AuthViewModel", "Login error: ${error::class.qualifiedName} | ${error.message}", error)
                    _loginUiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = AuthErrorMapper.mapThrowable(error),
                        )
                    }
                },
            )
        }
    }

    // --- Register Handlers ---

    fun onRegisterNameChanged(name: String) {
        _registerUiState.update { it.copy(name = name, nameError = null, generalError = null) }
    }

    fun onRegisterUsernameChanged(username: String) {
        _registerUiState.update { it.copy(username = username, usernameError = null, generalError = null) }
    }

    fun onRegisterEmailChanged(email: String) {
        _registerUiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onRegisterPasswordChanged(password: String) {
        _registerUiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun onRegisterConfirmPasswordChanged(confirmPassword: String) {
        _registerUiState.update {
            it.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = null,
                generalError = null,
            )
        }
    }

    fun toggleRegisterPasswordVisibility() {
        _registerUiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleRegisterConfirmPasswordVisibility() {
        _registerUiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun onTermsAcceptedChanged(accepted: Boolean) {
        _registerUiState.update { it.copy(termsAccepted = accepted, termsError = null, generalError = null) }
    }

    fun setTermsDialogOpen(isOpen: Boolean) {
        _registerUiState.update { it.copy(isTermsDialogOpen = isOpen) }
    }

    fun register(onSuccess: () -> Unit = {}) {
        val state = _registerUiState.value
        if (state.isLoading) return

        val nameError = AuthValidator.validateName(state.name)
        val usernameError = AuthValidator.validateUsername(state.username)
        val emailError = AuthValidator.validateOptionalEmail(state.email)
        val passwordError = AuthValidator.validatePassword(state.password)
        val confirmError = AuthValidator.validateConfirmPassword(state.password, state.confirmPassword)
        val termsError = AuthValidator.validateTermsAccepted(state.termsAccepted)

        if (nameError != null || usernameError != null || emailError != null || passwordError != null || confirmError != null || termsError != null) {
            _registerUiState.update {
                it.copy(
                    nameError = nameError,
                    usernameError = usernameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                    termsError = termsError,
                )
            }
            return
        }

        _registerUiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            val result = authRepository.register(
                name = state.name,
                username = state.username,
                password = state.password,
                email = state.email.ifBlank { null },
            )
            result.fold(
                onSuccess = {
                    _registerUiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { error ->
                    _registerUiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = AuthErrorMapper.mapThrowable(error),
                        )
                    }
                },
            )
        }
    }

    // --- Forgot Password OTP Handlers ---

    fun onForgotPasswordUsernameChanged(username: String) {
        _forgotPasswordUiState.update {
            it.copy(username = username, usernameError = null, generalError = null)
        }
    }

    fun onForgotPasswordCodeChanged(code: String) {
        _forgotPasswordUiState.update {
            it.copy(code = code, codeError = null, generalError = null)
        }
    }

    fun onForgotPasswordNewPasswordChanged(password: String) {
        _forgotPasswordUiState.update {
            it.copy(newPassword = password, newPasswordError = null, generalError = null)
        }
    }

    fun onForgotPasswordConfirmNewPasswordChanged(confirm: String) {
        _forgotPasswordUiState.update {
            it.copy(confirmNewPassword = confirm, confirmNewPasswordError = null, generalError = null)
        }
    }

    fun toggleNewPasswordVisibility() {
        _forgotPasswordUiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
    }

    fun toggleConfirmNewPasswordVisibility() {
        _forgotPasswordUiState.update { it.copy(isConfirmNewPasswordVisible = !it.isConfirmNewPasswordVisible) }
    }

    fun requestCode() {
        val state = _forgotPasswordUiState.value
        if (state.isLoading) return

        val usernameError = AuthValidator.validateUsername(state.username)
        if (usernameError != null) {
            _forgotPasswordUiState.update { it.copy(usernameError = usernameError) }
            return
        }

        _forgotPasswordUiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            authRepository.requestPasswordResetCode(state.username)
                .fold(
                    onSuccess = { hasEmail ->
                        _forgotPasswordUiState.update {
                            it.copy(
                                isLoading = false,
                                hasEmail = hasEmail,
                                step = if (hasEmail) ForgotPasswordStep.ENTER_CODE else ForgotPasswordStep.ENTER_USERNAME,
                                generalError = if (!hasEmail) com.shorashim.R.string.auth_error_not_configured else null,
                            )
                        }
                    },
                    onFailure = { error ->
                        _forgotPasswordUiState.update {
                            it.copy(
                                isLoading = false,
                                generalError = AuthErrorMapper.mapThrowable(error),
                            )
                        }
                    },
                )
        }
    }

    fun verifyCode() {
        val state = _forgotPasswordUiState.value
        if (state.isLoading) return

        val codeError = AuthValidator.validateResetCode(state.code)
        if (codeError != null) {
            _forgotPasswordUiState.update { it.copy(codeError = codeError) }
            return
        }

        // Advance to step 3 (enter new password)
        _forgotPasswordUiState.update {
            it.copy(step = ForgotPasswordStep.ENTER_NEW_PASSWORD, generalError = null)
        }
    }

    fun completePasswordReset(onSuccess: () -> Unit = {}) {
        val state = _forgotPasswordUiState.value
        if (state.isLoading) return

        val passwordError = AuthValidator.validatePassword(state.newPassword)
        val confirmError = AuthValidator.validateConfirmPassword(state.newPassword, state.confirmNewPassword)

        if (passwordError != null || confirmError != null) {
            _forgotPasswordUiState.update {
                it.copy(
                    newPasswordError = passwordError,
                    confirmNewPasswordError = confirmError,
                )
            }
            return
        }

        _forgotPasswordUiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            authRepository.verifyAndResetPassword(state.username, state.code, state.newPassword)
                .fold(
                    onSuccess = {
                        _forgotPasswordUiState.update {
                            it.copy(
                                isLoading = false,
                                step = ForgotPasswordStep.SUCCESS,
                                generalError = null,
                            )
                        }
                        onSuccess()
                    },
                    onFailure = { error ->
                        _forgotPasswordUiState.update {
                            it.copy(
                                isLoading = false,
                                generalError = AuthErrorMapper.mapThrowable(error),
                            )
                        }
                    },
                )
        }
    }

    fun resetForgotPasswordState() {
        _forgotPasswordUiState.value = ForgotPasswordUiState()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
