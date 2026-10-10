package com.shorashim

import com.shorashim.core.model.AuthState
import com.shorashim.core.model.AuthUser
import com.shorashim.data.auth.AuthRepository
import com.shorashim.feature.auth.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthViewModelTest {

    private class FakeAuthRepository : AuthRepository {
        val mutableAuthState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
        override val authState: StateFlow<AuthState> = mutableAuthState

        override suspend fun login(email: String, password: String): Result<AuthUser> {
            return Result.success(AuthUser("uid-1", email, "משתמש בדיקה"))
        }

        override suspend fun register(name: String, email: String, password: String): Result<AuthUser> {
            return Result.success(AuthUser("uid-1", email, name))
        }

        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun logout() {
            mutableAuthState.value = AuthState.Unauthenticated
        }
    }

    @Test
    fun loginValidationStopsWhenFieldsEmpty() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.login()

        val state = viewModel.loginUiState.value
        assertEquals(R.string.validation_email_empty, state.emailError)
        assertEquals(R.string.validation_password_empty, state.passwordError)
    }

    @Test
    fun registerValidationStopsWhenFieldsInvalid() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.onRegisterNameChanged("א")
        viewModel.onRegisterEmailChanged("bad-email")
        viewModel.onRegisterPasswordChanged("123")
        viewModel.onRegisterConfirmPasswordChanged("456")
        viewModel.onTermsAcceptedChanged(false)

        viewModel.register()

        val state = viewModel.registerUiState.value
        assertEquals(R.string.validation_name_too_short, state.nameError)
        assertEquals(R.string.validation_email_invalid, state.emailError)
        assertEquals(R.string.validation_password_too_short, state.passwordError)
        assertEquals(R.string.validation_passwords_do_not_match, state.confirmPasswordError)
        assertEquals(R.string.validation_terms_required, state.termsError)
    }

    @Test
    fun forgotPasswordValidationStopsOnInvalidEmail() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.onForgotPasswordEmailChanged("not-an-email")
        viewModel.sendPasswordResetEmail()

        val state = viewModel.forgotPasswordUiState.value
        assertEquals(R.string.validation_email_invalid, state.emailError)
    }
}
