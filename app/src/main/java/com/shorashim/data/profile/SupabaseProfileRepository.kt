package com.shorashim.data.profile

import android.util.Log
import com.shorashim.core.model.UserProfile
import com.shorashim.core.model.UserProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
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
                    fullName = dto.fullName.ifBlank { metaFullName },
                    displayName = dto.displayName.ifBlank { metaDisplayName },
                    avatarUrl = dto.avatarUrl,
                )
            } else {
                UserProfile(
                    id = currentUser.id,
                    fullName = metaFullName,
                    displayName = metaDisplayName,
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
                fullName = trimmedFullName,
                displayName = trimmedDisplayName,
            )
        }.onFailure { e ->
            Log.e("ProfileRepo", "Failed to update profile", e)
        }
    }
}
