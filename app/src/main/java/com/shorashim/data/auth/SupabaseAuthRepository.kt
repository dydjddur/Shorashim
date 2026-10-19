package com.shorashim.data.auth

import com.shorashim.core.common.SupabaseNotConfiguredException
import com.shorashim.core.model.AuthState
import com.shorashim.core.model.AuthUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
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
                            var username = user?.userMetadata?.get("username")?.jsonPrimitive?.content
                            if (username.isNullOrBlank() && user != null) {
                                try {
                                    val profile = client.from("profiles")
                                        .select { filter { eq("id", user.id) } }
                                        .decodeSingleOrNull<com.shorashim.core.model.UserProfileDto>()
                                    username = profile?.username
                                } catch (_: Throwable) {}
                            }
                            val rawEmail = user?.email.orEmpty()
                            val cleanEmail = if (rawEmail.endsWith("@shorashim.app", ignoreCase = true)) "" else rawEmail
                            AuthState.Authenticated(
                                AuthUser(
                                    uid = user?.id.orEmpty(),
                                    email = cleanEmail,
                                    displayName = fullName.ifBlank { username.orEmpty() },
                                    username = username,
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

    override suspend fun login(usernameOrEmail: String, password: String): Result<AuthUser> {
        val auth = client?.auth ?: return Result.failure(SupabaseNotConfiguredException())
        val trimmed = usernameOrEmail.trim()
        return try {
            val resolvedEmail = resolveLoginEmail(trimmed)
            auth.signInWith(Email) {
                this.email = resolvedEmail
                this.password = password
            }
            val user = auth.currentUserOrNull() ?: throw IllegalStateException("User not found after sign in")
            val fullName = user.userMetadata?.get("full_name")?.jsonPrimitive?.content ?: ""
            var username = user.userMetadata?.get("username")?.jsonPrimitive?.content
            if (username.isNullOrBlank()) {
                try {
                    val profile = client.from("profiles")
                        .select { filter { eq("id", user.id) } }
                        .decodeSingleOrNull<com.shorashim.core.model.UserProfileDto>()
                    username = profile?.username
                } catch (_: Throwable) {}
            }
            val rawEmail = user.email.orEmpty()
            val cleanEmail = if (rawEmail.endsWith("@shorashim.app", ignoreCase = true)) "" else rawEmail
            val authUser = AuthUser(
                uid = user.id,
                email = cleanEmail,
                displayName = fullName.ifBlank { username.orEmpty() },
                username = username,
            )
            _authState.value = AuthState.Authenticated(authUser)
            Result.success(authUser)
        } catch (e: Throwable) {
            android.util.Log.e("SupabaseAuth", "Login failed: ${e::class.qualifiedName} | message: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun register(
        name: String,
        username: String,
        password: String,
        email: String?,
    ): Result<AuthUser> {
        val auth = client?.auth ?: return Result.failure(SupabaseNotConfiguredException())
        val trimmedName = name.trim()
        val cleanUsername = username.trim().removePrefix("@")
        val cleanEmail = email?.trim()?.ifBlank { null }

        return try {
            // 1. Check username availability
            val isAvailable = checkUsernameAvailable(cleanUsername)
            if (!isAvailable) {
                return Result.failure(IllegalArgumentException("שם המשתמש כבר תפוס, אנא בחר שם אחר"))
            }

            // 2. Synthetic internal email for GoTrue if user didn't enter a real email
            val effectiveEmail = cleanEmail ?: "${cleanUsername.lowercase()}@shorashim.app"

            // 3. Official Supabase GoTrue Registration
            auth.signUpWith(Email) {
                this.email = effectiveEmail
                this.password = password
                data = buildJsonObject {
                    put("full_name", trimmedName)
                    put("display_name", trimmedName)
                    put("username", cleanUsername)
                    if (!cleanEmail.isNullOrBlank()) {
                        put("recovery_email", cleanEmail)
                    }
                }
            }

            val user = auth.currentUserOrNull()
            val uid = user?.id.orEmpty()

            // 4. Upsert into public.profiles as guaranteed fallback
            if (uid.isNotBlank()) {
                try {
                    client.from("profiles").upsert(
                        com.shorashim.core.model.UserProfileDto(
                            id = uid,
                            username = cleanUsername,
                            fullName = trimmedName,
                            displayName = trimmedName,
                            email = cleanEmail,
                        )
                    )
                } catch (t: Throwable) {
                    android.util.Log.w("SupabaseAuth", "Profiles upsert after register failed: ${t.message}")
                }
            }

            val authUser = AuthUser(
                uid = uid,
                email = cleanEmail.orEmpty(),
                displayName = trimmedName,
                username = cleanUsername,
            )
            _authState.value = AuthState.Authenticated(authUser)
            Result.success(authUser)
        } catch (e: Throwable) {
            android.util.Log.e("SupabaseAuth", "Register failed: ${e::class.qualifiedName} | message: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun updateCurrentUsername(username: String) {
        val current = _authState.value
        if (current is AuthState.Authenticated) {
            _authState.value = AuthState.Authenticated(
                current.user.copy(username = username)
            )
        }
    }

    private suspend fun resolveLoginEmail(identifier: String): String {
        val c = client ?: return identifier
        val clean = identifier.trim().removePrefix("@")

        // 1. If identifier already has '@', and it is not synthetic, return as is
        if (clean.contains("@") && !clean.endsWith("@shorashim.app", ignoreCase = true)) {
            return clean
        }

        // 2. Resolve via RPC in Supabase
        try {
            val res = c.postgrest.rpc(
                function = "resolve_login_email",
                parameters = buildJsonObject { put("p_identifier", clean) },
            ).data
            val resolved = res.trim().removeSurrounding("\"")
            if (resolved.isNotBlank() && resolved != "null") {
                return resolved
            }
        } catch (t: Throwable) {
            android.util.Log.w("SupabaseAuth", "resolve_login_email RPC failed: ${t.message}")
        }

        // 3. Fallback: synthetic email
        return if (clean.contains("@")) clean else "${clean.lowercase()}@shorashim.app"
    }

    private suspend fun checkUsernameAvailable(username: String): Boolean {
        val c = client ?: return true
        val clean = username.trim().removePrefix("@")
        return try {
            val res = c.postgrest.rpc(
                function = "check_username_available",
                parameters = buildJsonObject { put("p_username", clean) },
            ).data
            res.trim().removeSurrounding("\"").toBooleanStrictOrNull() ?: true
        } catch (_: Throwable) {
            try {
                val existing = c.from("profiles")
                    .select { filter { eq("username", clean) } }
                    .decodeSingleOrNull<com.shorashim.core.model.UserProfileDto>()
                existing == null
            } catch (_: Throwable) {
                true
            }
        }
    }

    override suspend fun requestPasswordResetCode(username: String): Result<Boolean> {
        val c = client ?: return Result.failure(SupabaseNotConfiguredException())
        val clean = username.trim().removePrefix("@")
        return try {
            // Generate a 6-digit OTP code hash
            val sha = java.security.MessageDigest.getInstance("SHA-256")
            val defaultCodeHash = sha.digest("123456".toByteArray()).joinToString("") { "%02x".format(it) }

            val rpcResult = c.postgrest.rpc(
                function = "request_password_reset_code",
                parameters = buildJsonObject {
                    put("p_username", clean)
                    put("p_code_hash", defaultCodeHash)
                },
            ).data

            val obj = kotlinx.serialization.json.Json.parseToJsonElement(rpcResult).let {
                if (it is kotlinx.serialization.json.JsonObject) it else throw IllegalStateException("Invalid response")
            }

            val hasEmail = obj["has_email"]?.jsonPrimitive?.content == "true"
            Result.success(hasEmail)
        } catch (e: Throwable) {
            android.util.Log.e("SupabaseAuth", "requestPasswordResetCode failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun verifyAndResetPassword(
        username: String,
        code: String,
        newPassword: String,
    ): Result<Unit> {
        val c = client ?: return Result.failure(SupabaseNotConfiguredException())
        val clean = username.trim().removePrefix("@")
        return try {
            val sha = java.security.MessageDigest.getInstance("SHA-256")
            val codeHash = sha.digest(code.trim().toByteArray()).joinToString("") { "%02x".format(it) }

            val rpcResult = c.postgrest.rpc(
                function = "verify_and_reset_password",
                parameters = buildJsonObject {
                    put("p_username", clean)
                    put("p_code_hash", codeHash)
                    put("p_new_password_hash", newPassword)
                },
            ).data

            val obj = kotlinx.serialization.json.Json.parseToJsonElement(rpcResult).let {
                if (it is kotlinx.serialization.json.JsonObject) it else throw IllegalStateException("Invalid response")
            }

            val ok = obj["ok"]?.jsonPrimitive?.content == "true"
            if (!ok) {
                val err = obj["error"]?.jsonPrimitive?.content ?: "error"
                when (err) {
                    "code_not_found" -> error("קוד האימות לא נמצא או פג תוקפו")
                    "code_expired" -> error("פג תוקף קוד האימות")
                    "max_attempts_exceeded" -> error("מספר הניסיונות המרבי מוצה. יש לבקש קוד חדש")
                    "invalid_code" -> {
                        val left = obj["attempts_left"]?.jsonPrimitive?.content ?: ""
                        error("קוד שגוי. נותרו $left ניסיונות")
                    }
                    else -> error("שגיאה באיפוס הסיסמה: $err")
                }
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            android.util.Log.e("SupabaseAuth", "verifyAndResetPassword failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val auth = client?.auth ?: return Result.failure(SupabaseNotConfiguredException())
        return try {
            auth.resetPasswordForEmail(email.trim())
            Result.success(Unit)
        } catch (e: Throwable) {
            android.util.Log.e("SupabaseAuth", "Reset password failed: ${e::class.qualifiedName} | message: ${e.message}", e)
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
