package com.shorashim.data.auth

import com.shorashim.core.model.AuthState
import com.shorashim.core.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for authentication operations and session state.
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun login(email: String, password: String): Result<AuthUser>

    suspend fun register(name: String, email: String, password: String): Result<AuthUser>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun logout()
}
