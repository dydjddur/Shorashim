package com.shorashim.data.auth

import com.shorashim.core.model.AuthState
import com.shorashim.core.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for authentication operations and session state.
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun login(usernameOrEmail: String, password: String): Result<AuthUser>

    suspend fun register(
        name: String,
        username: String,
        password: String,
        email: String? = null,
    ): Result<AuthUser>

    suspend fun requestPasswordResetCode(username: String): Result<Boolean>

    suspend fun verifyAndResetPassword(username: String, code: String, newPassword: String): Result<Unit>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun logout()

    fun updateCurrentUsername(username: String) {}
}
