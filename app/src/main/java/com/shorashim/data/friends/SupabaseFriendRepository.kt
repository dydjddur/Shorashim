package com.shorashim.data.friends

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.shorashim.core.model.Family
import com.shorashim.core.model.FamilyDto
import com.shorashim.core.model.FamilyStatus
import com.shorashim.core.model.FriendRequest
import com.shorashim.core.model.FriendSuggestion
import com.shorashim.core.model.FriendUser
import com.shorashim.core.model.UserProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class SupabaseFriendRepository(
    private val supabase: SupabaseClient?,
    context: Context,
) : FriendRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("shorashim_friends_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    init {
        try {
            val rawFriends = prefs.getString("local_friends", null)
            if (!rawFriends.isNullOrBlank()) {
                val list = runCatching { json.decodeFromString<List<FriendUser>>(rawFriends) }.getOrDefault(emptyList())
                val cleaned = list.filterNot { it.id.startsWith("friend_default_") }
                if (cleaned.size != list.size) {
                    prefs.edit().putString("local_friends", json.encodeToString(cleaned)).apply()
                }
            }
            val rawReqs = prefs.getString("pending_requests", null)
            if (!rawReqs.isNullOrBlank()) {
                val list = runCatching { json.decodeFromString<List<FriendRequest>>(rawReqs) }.getOrDefault(emptyList())
                val cleaned = list.filterNot { it.id == "req_1" || it.fromUserId == "usr_101" }
                if (cleaned.size != list.size) {
                    prefs.edit().putString("pending_requests", json.encodeToString(cleaned)).apply()
                }
            }
            val friendIds = prefs.getStringSet("friend_ids", null)
            if (friendIds != null) {
                val cleaned = friendIds.filterNot { it.startsWith("friend_default_") || it in setOf("avi_levi", "sara_israeli") }.toSet()
                if (cleaned.size != friendIds.size) {
                    prefs.edit().putStringSet("friend_ids", cleaned).apply()
                }
            }
        } catch (_: Throwable) {}
    }

    override suspend fun getFriends(): Result<List<FriendUser>> {
        val client = supabase ?: return Result.success(getLocalFriends())
        val currentUserId = client.auth.currentUserOrNull()?.id

        return runCatching {
            val localFriendIds = getLocalFriendIdSet()
            if (localFriendIds.isEmpty()) {
                return@runCatching getLocalFriends()
            }

            // Fetch profiles from Supabase
            val profiles = try {
                client.from("profiles")
                    .select()
                    .decodeList<UserProfileDto>()
                    .filter { it.id != currentUserId }
            } catch (e: Throwable) {
                Log.w("FriendRepo", "Could not query profiles for friends, using local: ${e.message}")
                emptyList()
            }

            if (profiles.isNotEmpty()) {
                val friendsList = mutableListOf<FriendUser>()
                for (p in profiles) {
                    val isExplicitFriend = localFriendIds.contains(p.id) || (!p.username.isNullOrBlank() && localFriendIds.contains(p.username))
                    if (isExplicitFriend) {
                        // Count their trees
                        val treeCount = try {
                            client.from("families")
                                .select { filter { eq("owner_id", p.id) } }
                                .decodeList<FamilyDto>()
                                .size
                        } catch (_: Throwable) {
                            0
                        }

                        friendsList.add(
                            FriendUser(
                                id = p.id,
                                username = p.username,
                                displayName = p.displayName.ifBlank { p.fullName.ifBlank { p.username ?: "משתמש" } },
                                avatarUrl = p.avatarUrl,
                                isOnline = false,
                                lastSeenText = "חבר באפליקציה",
                                isPrivateProfile = p.isPrivate,
                                treeCount = treeCount,
                            )
                        )
                    }
                }
                if (friendsList.isNotEmpty()) {
                    return@runCatching friendsList
                }
            }
            getLocalFriends()
        }
    }

    override suspend fun getFriendSuggestions(): Result<List<FriendSuggestion>> {
        val client = supabase ?: return Result.success(getDefaultSuggestions())
        val currentUserId = client.auth.currentUserOrNull()?.id

        return runCatching {
            val profiles = try {
                client.from("profiles")
                    .select()
                    .decodeList<UserProfileDto>()
                    .filter { it.id != currentUserId }
            } catch (e: Throwable) {
                Log.w("FriendRepo", "Could not query profiles for suggestions: ${e.message}")
                emptyList()
            }

            val suggestions = mutableListOf<FriendSuggestion>()
            val localFriendIds = getLocalFriendIdSet()

            for (p in profiles) {
                if (localFriendIds.contains(p.id) || localFriendIds.contains(p.username)) continue

                val reasons = listOf(
                    "שם משפחה תואם בקהילה",
                    "חבר משותף",
                    "קרוב משפחה אפשרי לפי שושלת",
                    "פעיל בעצי קהילה",
                )
                val reasonIndex = kotlin.math.abs(p.id.hashCode()) % reasons.size

                suggestions.add(
                    FriendSuggestion(
                        id = p.id,
                        username = p.username,
                        displayName = p.displayName.ifBlank { p.fullName.ifBlank { p.username ?: "משתמש" } },
                        avatarUrl = p.avatarUrl,
                        reason = reasons[reasonIndex],
                    )
                )
            }

            suggestions
        }
    }

    override suspend fun getPendingRequests(): Result<List<FriendRequest>> {
        return runCatching {
            val raw = prefs.getString("pending_requests", null)
            if (!raw.isNullOrBlank()) {
                val list = runCatching { json.decodeFromString<List<FriendRequest>>(raw) }.getOrDefault(emptyList())
                list.filterNot { it.id == "req_1" || it.fromUserId == "usr_101" }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun sendFriendRequest(targetUserIdOrUsername: String): Result<Unit> {
        return runCatching {
            val clean = targetUserIdOrUsername.trim().removePrefix("@")
            val sent = prefs.getStringSet("sent_requests", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            sent.add(clean)
            prefs.edit().putStringSet("sent_requests", sent).apply()
            Unit
        }
    }

    override suspend fun respondToFriendRequest(requestId: String, accept: Boolean): Result<Unit> {
        return runCatching {
            val currentRequests = getPendingRequests().getOrDefault(emptyList()).toMutableList()
            val req = currentRequests.firstOrNull { it.id == requestId }
            if (req != null) {
                currentRequests.removeAll { it.id == requestId }
                prefs.edit().putString("pending_requests", json.encodeToString(currentRequests)).apply()

                if (accept) {
                    val friends = getLocalFriends().toMutableList()
                    friends.add(
                        FriendUser(
                            id = req.fromUserId,
                            username = req.fromUsername,
                            displayName = req.fromDisplayName,
                            avatarUrl = req.fromAvatarUrl,
                            isOnline = true,
                            lastSeenText = "מחובר עכשיו",
                            treeCount = 1,
                        )
                    )
                    saveLocalFriends(friends)
                    val friendIds = getLocalFriendIdSet().toMutableSet()
                    friendIds.add(req.fromUserId)
                    if (!req.fromUsername.isNullOrBlank()) friendIds.add(req.fromUsername)
                    prefs.edit().putStringSet("friend_ids", friendIds).apply()
                }
            }
            Unit
        }
    }

    override suspend fun removeFriend(friendUserId: String): Result<Unit> {
        return runCatching {
            val friends = getLocalFriends().filter { it.id != friendUserId }
            saveLocalFriends(friends)
            val friendIds = getLocalFriendIdSet().toMutableSet()
            friendIds.remove(friendUserId)
            prefs.edit().putStringSet("friend_ids", friendIds).apply()
            Unit
        }
    }

    override suspend fun getFriendTrees(friendUserId: String): Result<List<Family>> {
        val client = supabase ?: return Result.success(emptyList())

        return runCatching {
            // First check if friend has a private profile
            val profile = try {
                client.from("profiles")
                    .select { filter { eq("id", friendUserId) } }
                    .decodeSingleOrNull<UserProfileDto>()
            } catch (_: Throwable) {
                null
            }

            if (profile?.isPrivate == true) {
                // If profile is set to private, do not return their trees
                return@runCatching emptyList()
            }

            val dtos = client.from("families")
                .select {
                    filter {
                        eq("owner_id", friendUserId)
                    }
                }
                .decodeList<FamilyDto>()

            dtos.map { dto ->
                Family(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    ownerId = dto.ownerId,
                    status = dto.status,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                )
            }
        }
    }

    private fun getLocalFriendIdSet(): Set<String> {
        return prefs.getStringSet("friend_ids", emptySet()) ?: emptySet()
    }

    private fun getLocalFriends(): List<FriendUser> {
        val raw = prefs.getString("local_friends", null)
        if (!raw.isNullOrBlank()) {
            val list = runCatching { json.decodeFromString<List<FriendUser>>(raw) }.getOrDefault(emptyList())
            return list.filterNot { it.id.startsWith("friend_default_") }
        }
        return emptyList()
    }

    private fun saveLocalFriends(friends: List<FriendUser>) {
        prefs.edit().putString("local_friends", json.encodeToString(friends)).apply()
    }

    private fun getDefaultSuggestions(): List<FriendSuggestion> {
        return emptyList()
    }
}
