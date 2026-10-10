package com.shorashim.data.auth

import com.shorashim.core.common.SupabaseNotConfiguredException
import com.shorashim.core.model.AuthState
import com.shorashim.core.model.AuthUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Supabase-backed implementation of AuthRepository.
 * Observes Supabase GoTrue sessionStatus and provides login, registration,
 * password reset, and logout operations.
 */
class SupabaseAuthRepository(
    private val client: SupabaseClient?,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(
        if (client == null) {
            AuthState.Unconfigured("פרטי Supabase חסרים בקובץ .env")
        } else {
            AuthState.Loading
        }
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        if (client != null) {
            scope.launch {
                client.auth.sessionStatus.collect { status ->
                    _authState.value = when (status) {
                        is SessionStatus.Authenticated -> {
                            val user = status.session.user
                            val fullName = user?.userMetadata?.get("full_name")?.jsonPrimitive?.content ?: ""
                            AuthState.Authenticated(
                                AuthUser(
                                    uid = user?.id.orEmpty(),
                                    email = user?.email.orEmpty(),
                                    displayName = fullName,
                                ),
                            )
                        }
                        is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated
                        is SessionStatus.Initializing -> AuthState.Loading
                        else -> AuthState.Unauthenticated
                    }
                }
            }
        }
    }

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        val auth = client?.auth ?: return Result.failure(SupabaseNotConfiguredException())
        return try {
            auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val user = auth.currentUserOrNull() ?: throw IllegalStateException("User not found after sign in")
            val fullName = user.userMetadata?.get("full_name")?.jsonPrimitive?.content ?: ""
            Result.success(
                AuthUser(
                    uid = user.id,
                    email = user.email.orEmpty(),
                    displayName = fullName,
                ),
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<AuthUser> {
        val auth = client?.auth ?: return Result.failure(SupabaseNotConfiguredException())
        return try {
            val trimmedName = name.trim()
            auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                if (trimmedName.isNotEmpty()) {
                    data = buildJsonObject {
                        put("full_name", trimmedName)
                    }
                }
            }
            val user = auth.currentUserOrNull()
            val fullName = user?.userMetadata?.get("full_name")?.jsonPrimitive?.content ?: trimmedName
            Result.success(
                AuthUser(
                    uid = user?.id.orEmpty(),
                    email = user?.email.orEmpty().ifEmpty { email.trim() },
                    displayName = fullName,
                ),
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val auth = client?.auth ?: return Result.failure(SupabaseNotConfiguredException())
        return try {
            auth.resetPasswordForEmail(email.trim())
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        val auth = client?.auth
        if (auth != null) {
            try {
                auth.signOut()
            } catch (_: Throwable) {}
            _authState.value = AuthState.Unauthenticated
        } else {
            _authState.value = AuthState.Unconfigured("פרטי Supabase חסרים בקובץ .env")
        }
    }
}
