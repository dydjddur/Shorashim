package com.shorashim.data.profile

import android.util.Log
import com.shorashim.core.model.UserProfile
import com.shorashim.core.model.UserProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseProfileRepository(
    private val supabase: SupabaseClient?,
) : ProfileRepository {

    override suspend fun getProfile(): Result<UserProfile> {
        val client = supabase ?: return Result.failure(IllegalStateException("Supabase not configured"))
        val auth = client.auth
        val currentUser = auth.currentUserOrNull() ?: return Result.failure(IllegalStateException("Not authenticated"))

        val metaFullName = currentUser.userMetadata?.get("full_name")?.jsonPrimitive?.content ?: ""
        val metaDisplayName = currentUser.userMetadata?.get("display_name")?.jsonPrimitive?.content ?: metaFullName

        return runCatching {
            val dto = try {
                client.from("profiles")
                    .select {
                        filter {
                            eq("id", currentUser.id)
                        }
                    }
                    .decodeSingleOrNull<UserProfileDto>()
            } catch (e: Throwable) {
                Log.w("ProfileRepo", "Could not query profiles table, falling back to auth metadata: ${e.message}")
                null
            }

            if (dto != null) {
                UserProfile(
                    id = dto.id,
                    username = dto.username,
                    fullName = dto.fullName.ifBlank { metaFullName },
                    displayName = dto.displayName.ifBlank { metaDisplayName },
                    avatarUrl = dto.avatarUrl,
                    email = dto.email ?: currentUser.email,
                )
            } else {
                UserProfile(
                    id = currentUser.id,
                    username = currentUser.userMetadata?.get("username")?.jsonPrimitive?.content,
                    fullName = metaFullName,
                    displayName = metaDisplayName,
                    email = currentUser.email,
                )
            }
        }
    }

    override suspend fun updateProfile(fullName: String, displayName: String): Result<UserProfile> {
        val client = supabase ?: return Result.failure(IllegalStateException("Supabase not configured"))
        val auth = client.auth
        val currentUser = auth.currentUserOrNull() ?: return Result.failure(IllegalStateException("Not authenticated"))
        val trimmedFullName = fullName.trim()
        val trimmedDisplayName = displayName.trim().ifBlank { trimmedFullName }

        return runCatching {
            // 1. Update Supabase GoTrue user metadata
            auth.updateUser {
                data = buildJsonObject {
                    put("full_name", trimmedFullName)
                    put("display_name", trimmedDisplayName)
                }
            }

            // 2. Upsert into public.profiles
            try {
                val profileDto = UserProfileDto(
                    id = currentUser.id,
                    fullName = trimmedFullName,
                    displayName = trimmedDisplayName,
                )
                client.from("profiles").upsert(profileDto) {
                    select()
                }
            } catch (e: Throwable) {
                Log.w("ProfileRepo", "profiles table upsert failed (will rely on auth metadata): ${e.message}")
            }

            UserProfile(
                id = currentUser.id,
                username = currentUser.userMetadata?.get("username")?.jsonPrimitive?.content,
                fullName = trimmedFullName,
                displayName = trimmedDisplayName,
                email = currentUser.email,
            )
        }.onFailure { e ->
            Log.e("ProfileRepo", "Failed to update profile", e)
        }
    }

    override suspend fun searchUserByUsername(username: String): Result<UserProfile?> {
        val client = supabase ?: return Result.failure(IllegalStateException("Supabase not configured"))
        val clean = username.trim().removePrefix("@")
        if (clean.isBlank()) return Result.success(null)

        return runCatching {
            try {
                val rpcResult = client.from("profiles")
                    .select {
                        filter {
                            eq("username", clean)
                        }
                    }
                    .decodeSingleOrNull<UserProfileDto>()

                if (rpcResult != null) {
                    UserProfile(
                        id = rpcResult.id,
                        username = rpcResult.username,
                        fullName = rpcResult.fullName,
                        displayName = rpcResult.displayName.ifBlank { rpcResult.fullName.ifBlank { rpcResult.username.orEmpty() } },
                        avatarUrl = rpcResult.avatarUrl,
                    )
                } else {
                    null
                }
            } catch (t: Throwable) {
                Log.w("ProfileRepo", "searchUserByUsername fallback error: ${t.message}")
                null
            }
        }
    }

    override suspend fun setUsername(username: String): Result<Unit> {
        val client = supabase ?: return Result.failure(IllegalStateException("Supabase not configured"))
        val clean = username.trim().removePrefix("@")
        return runCatching {
            val res = client.postgrest.rpc(
                function = "set_my_username",
                parameters = buildJsonObject { put("p_username", clean) },
            ).data

            val obj = kotlinx.serialization.json.Json.parseToJsonElement(res)
            val ok = obj.let {
                if (it is kotlinx.serialization.json.JsonObject) {
                    it["ok"]?.jsonPrimitive?.content == "true"
                } else false
            }
            if (!ok) {
                val err = if (obj is kotlinx.serialization.json.JsonObject) obj["error"]?.jsonPrimitive?.content else "error"
                if (err == "username_taken") error("שם המשתמש כבר תפוס")
                else if (err == "invalid_username_format") error("שם המשתמש חייב להכיל 3-30 תווים (אותיות באנגלית, מספרים וקו תחתון)")
                else error("שגיאה בהגדרת שם המשתמש: $err")
            }
            Unit
        }
    }
}
