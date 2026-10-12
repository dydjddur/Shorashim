package com.shorashim.core.model

/**
 * Domain representation of an authenticated user.
 */
data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String = "",
    val username: String? = null,
)
