package com.shorashim.core.model

/**
 * Represents the persistent authentication session state.
 */
sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
    data class Unconfigured(val message: String) : AuthState
}
