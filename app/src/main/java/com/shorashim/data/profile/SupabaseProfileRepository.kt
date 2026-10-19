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
        val metaAvatarUrl = currentUser.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
        val metaIsPrivate = currentUser.userMetadata?.get("is_private")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        val metaBio = currentUser.userMetadata?.get("bio")?.jsonPrimitive?.content

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
                    avatarUrl = dto.avatarUrl ?: metaAvatarUrl,
                    email = dto.email ?: currentUser.email,
                    isPrivate = dto.isPrivate || metaIsPrivate,
                    bio = dto.bio ?: metaBio,
                )
            } else {
                UserProfile(
                    id = currentUser.id,
                    username = currentUser.userMetadata?.get("username")?.jsonPrimitive?.content,
                    fullName = metaFullName,
                    displayName = metaDisplayName,
                    avatarUrl = metaAvatarUrl,
                    email = currentUser.email,
                    isPrivate = metaIsPrivate,
                    bio = metaBio,
                )
            }
        }
    }

    override suspend fun updateProfile(fullName: String, displayName: String): Result<UserProfile> {
        return updateProfile(fullName, displayName, avatarUrl = null, isPrivate = false, bio = null)
    }

    override suspend fun updateProfile(
        fullName: String,
        displayName: String,
        avatarUrl: String?,
        isPrivate: Boolean,
        bio: String?,
    ): Result<UserProfile> {
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
                    if (avatarUrl != null) put("avatar_url", avatarUrl)
                    put("is_private", isPrivate)
                    if (bio != null) put("bio", bio)
                }
            }

            // 2. Upsert into public.profiles
            try {
                val profileDto = UserProfileDto(
                    id = currentUser.id,
                    fullName = trimmedFullName,
                    displayName = trimmedDisplayName,
                    avatarUrl = avatarUrl,
                    isPrivate = isPrivate,
                    bio = bio,
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
                avatarUrl = avatarUrl,
                email = currentUser.email,
                isPrivate = isPrivate,
                bio = bio,
            )
        }.onFailure { e ->
            Log.e("ProfileRepo", "Failed to update profile", e)
        }
    }

    override suspend fun setPrivateAccountMode(isPrivate: Boolean): Result<Unit> {
        val profileRes = getProfile()
        val current = profileRes.getOrNull()
        return if (current != null) {
            updateProfile(
                fullName = current.fullName,
                displayName = current.displayName,
                avatarUrl = current.avatarUrl,
                isPrivate = isPrivate,
                bio = current.bio,
            ).map { }
        } else {
            val client = supabase ?: return Result.failure(IllegalStateException("Supabase not configured"))
            runCatching {
                client.auth.updateUser {
                    data = buildJsonObject {
                        put("is_private", isPrivate)
                    }
                }
                Unit
            }
        }
    }

    override suspend fun searchUserByUsername(username: String): Result<UserProfile?> {
        val client = supabase ?: return Result.failure(IllegalStateException("Supabase not configured"))
        val clean = username.trim().removePrefix("@").lowercase()
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

    override suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        val client = supabase ?: return Result.failure(IllegalStateException("Supabase not configured"))
        val q = query.trim().removePrefix("@").lowercase()
        if (q.isBlank()) return Result.success(emptyList())

        return runCatching {
            val me = client.auth.currentUserOrNull()?.id

            // 1. Try dedicated search RPC first
            val rpcList = runCatching {
                client.postgrest.rpc(
                    function = "search_app_users",
                    parameters = buildJsonObject { put("p_query", q) },
                ).decodeList<UserProfileDto>()
            }.getOrNull()

            val list = if (!rpcList.isNullOrEmpty()) {
                rpcList
            } else {
                client.from("profiles")
                    .select {
                        limit(100)
                    }
                    .decodeList<UserProfileDto>()
                    .filter { p ->
                        p.username?.lowercase()?.contains(q) == true ||
                        p.fullName.lowercase().contains(q) ||
                        p.displayName.lowercase().contains(q)
                    }
            }

            list.filter { p ->
                p.id != me && (!p.isPrivate || !p.username.isNullOrBlank())
            }.map { p ->
                UserProfile(
                    id = p.id,
                    username = p.username,
                    fullName = p.fullName,
                    displayName = p.displayName.ifBlank { p.fullName.ifBlank { p.username.orEmpty() } },
                    avatarUrl = p.avatarUrl,
                )
            }
        }
    }

    override suspend fun setUsername(username: String): Result<Unit> {
        val client = supabase ?: return Result.failure(IllegalStateException("Supabase not configured"))
        val clean = username.trim().removePrefix("@").lowercase()
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
