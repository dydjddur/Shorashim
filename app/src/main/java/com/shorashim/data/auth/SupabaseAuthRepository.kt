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
                            val username = user?.userMetadata?.get("username")?.jsonPrimitive?.content
                            AuthState.Authenticated(
                                AuthUser(
                                    uid = user?.id.orEmpty(),
                                    email = user?.email.orEmpty(),
                                    displayName = fullName,
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
            if (trimmed.contains("@")) {
                // Direct Email Sign-In
                auth.signInWith(Email) {
                    this.email = trimmed
                    this.password = password
                }
                val user = auth.currentUserOrNull() ?: throw IllegalStateException("User not found after sign in")
                val fullName = user.userMetadata?.get("full_name")?.jsonPrimitive?.content ?: ""
                val username = user.userMetadata?.get("username")?.jsonPrimitive?.content
                Result.success(
                    AuthUser(
                        uid = user.id,
                        email = user.email.orEmpty(),
                        displayName = fullName,
                        username = username,
                    ),
                )
            } else {
                // Username Sign-In: First find profile to check if user has email
                val cleanUsername = trimmed.removePrefix("@")
                var profileEmail: String? = null
                try {
                    val profileDto = client.from("profiles")
                        .select {
                            filter {
                                eq("username", cleanUsername)
                            }
                        }
                        .decodeSingleOrNull<com.shorashim.core.model.UserProfileDto>()
                    profileEmail = profileDto?.email
                } catch (_: Throwable) {}

                if (!profileEmail.isNullOrBlank()) {
                    auth.signInWith(Email) {
                        this.email = profileEmail
                        this.password = password
                    }
                    val user = auth.currentUserOrNull() ?: throw IllegalStateException("User not found after sign in")
                    val fullName = user.userMetadata?.get("full_name")?.jsonPrimitive?.content ?: ""
                    Result.success(
                        AuthUser(
                            uid = user.id,
                            email = user.email.orEmpty(),
                            displayName = fullName,
                            username = cleanUsername,
                        ),
                    )
                } else {
                    // Native username login via RPC
                    val rpcResult = client.postgrest.rpc(
                        function = "login_with_username",
                        parameters = buildJsonObject {
                            put("p_username_or_email", cleanUsername)
                            put("p_password", password)
                        },
                    ).data

                    val obj = kotlinx.serialization.json.Json.parseToJsonElement(rpcResult).let {
                        if (it is kotlinx.serialization.json.JsonObject) it else throw IllegalStateException("Invalid response from server")
                    }

                    val ok = obj["ok"]?.jsonPrimitive?.content == "true"
                    if (!ok) {
                        val err = obj["error"]?.jsonPrimitive?.content ?: "invalid_credentials"
                        when (err) {
                            "invalid_credentials" -> error("שם משתמש או סיסמה שגויים")
                            else -> error("שגיאת התחברות: $err")
                        }
                    }

                    val uid = obj["user_id"]?.jsonPrimitive?.content.orEmpty()
                    val disp = obj["display_name"]?.jsonPrimitive?.content.orEmpty()
                    val mail = obj["email"]?.jsonPrimitive?.content.orEmpty()

                    val authUser = AuthUser(
                        uid = uid,
                        email = mail,
                        displayName = disp,
                        username = cleanUsername,
                    )
                    _authState.value = AuthState.Authenticated(authUser)
                    Result.success(authUser)
                }
            }
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
            if (!cleanEmail.isNullOrBlank()) {
                auth.signUpWith(Email) {
                    this.email = cleanEmail
                    this.password = password
                    data = buildJsonObject {
                        put("full_name", trimmedName)
                        put("username", cleanUsername)
                    }
                }
                val user = auth.currentUserOrNull()
                val fullName = user?.userMetadata?.get("full_name")?.jsonPrimitive?.content ?: trimmedName

                // Upsert into profiles table to record the unique username
                try {
                    client.from("profiles").upsert(
                        com.shorashim.core.model.UserProfileDto(
                            id = user?.id.orEmpty(),
                            username = cleanUsername,
                            fullName = trimmedName,
                            displayName = trimmedName,
                            email = cleanEmail,
                        )
                    )
                } catch (t: Throwable) {
                    android.util.Log.w("SupabaseAuth", "Profiles upsert after register failed: ${t.message}")
                }

                Result.success(
                    AuthUser(
                        uid = user?.id.orEmpty(),
                        email = user?.email.orEmpty().ifEmpty { cleanEmail },
                        displayName = fullName,
                        username = cleanUsername,
                    ),
                )
            } else {
                // Native username registration without email
                val rpcResult = client.postgrest.rpc(
                    function = "register_with_username",
                    parameters = buildJsonObject {
                        put("p_name", trimmedName)
                        put("p_username", cleanUsername)
                        put("p_password", password)
                    },
                ).data

                val obj = kotlinx.serialization.json.Json.parseToJsonElement(rpcResult).let {
                    if (it is kotlinx.serialization.json.JsonObject) it else throw IllegalStateException("Invalid response from server")
                }

                val ok = obj["ok"]?.jsonPrimitive?.content == "true"
                if (!ok) {
                    val err = obj["error"]?.jsonPrimitive?.content ?: "unknown_error"
                    when (err) {
                        "username_taken" -> error("שם המשתמש כבר תפוס, אנא בחר שם אחר")
                        "invalid_username_format" -> error("שם משתמש לא תקין (3-30 תווים באנגלית, מספרים וקו תחתון)")
                        "password_too_short" -> error("הסיסמה חייבת להכיל לפחות 6 תווים")
                        else -> error("שגיאה ביצירת החשבון: $err")
                    }
                }

                val uid = obj["user_id"]?.jsonPrimitive?.content.orEmpty()
                val authUser = AuthUser(
                    uid = uid,
                    email = "",
                    displayName = trimmedName,
                    username = cleanUsername,
                )
                _authState.value = AuthState.Authenticated(authUser)
                Result.success(authUser)
            }
        } catch (e: Throwable) {
            android.util.Log.e("SupabaseAuth", "Register failed: ${e::class.qualifiedName} | message: ${e.message}", e)
            Result.failure(e)
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
