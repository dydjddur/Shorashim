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

    fun onLoginEmailChanged(email: String) {
        _loginUiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onLoginPasswordChanged(password: String) {
        _loginUiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun toggleLoginPasswordVisibility() {
        _loginUiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login(onSuccess: () -> Unit = {}) {
        val state = _loginUiState.value
        if (state.isLoading) return

        val emailError = AuthValidator.validateEmail(state.email)
        val passwordError = AuthValidator.validatePassword(state.password)

        if (emailError != null || passwordError != null) {
            _loginUiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                )
            }
            return
        }

        _loginUiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            val result = authRepository.login(state.email, state.password)
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
        val emailError = AuthValidator.validateEmail(state.email)
        val passwordError = AuthValidator.validatePassword(state.password)
        val confirmError = AuthValidator.validateConfirmPassword(state.password, state.confirmPassword)
        val termsError = AuthValidator.validateTermsAccepted(state.termsAccepted)

        if (nameError != null || emailError != null || passwordError != null || confirmError != null || termsError != null) {
            _registerUiState.update {
                it.copy(
                    nameError = nameError,
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
            val result = authRepository.register(state.name, state.email, state.password)
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

    // --- Forgot Password Handlers ---

    fun onForgotPasswordEmailChanged(email: String) {
        _forgotPasswordUiState.update {
            it.copy(email = email, emailError = null, generalError = null)
        }
    }

    fun sendPasswordResetEmail() {
        val state = _forgotPasswordUiState.value
        if (state.isLoading) return

        val emailError = AuthValidator.validateEmail(state.email)
        if (emailError != null) {
            _forgotPasswordUiState.update { it.copy(emailError = emailError) }
            return
        }

        _forgotPasswordUiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(state.email)
            result.fold(
                onSuccess = {
                    _forgotPasswordUiState.update {
                        it.copy(
                            isLoading = false,
                            isEmailSent = true,
                            generalError = null,
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

    fun resetForgotPasswordState() {
        _forgotPasswordUiState.value = ForgotPasswordUiState()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
