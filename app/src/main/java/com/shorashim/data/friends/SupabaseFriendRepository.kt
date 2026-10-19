package com.shorashim.data.friends

import android.content.Context
import android.util.Log
import com.shorashim.core.model.Family
import com.shorashim.core.model.FamilyDto
import com.shorashim.core.model.FriendRequest
import com.shorashim.core.model.FriendSuggestion
import com.shorashim.core.model.FriendUser
import com.shorashim.core.model.UserProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val TAG = "FriendRepo"

@Serializable
private data class FriendRowDto(
    @SerialName("friend_id") val friendId: String = "",
)

@Serializable
private data class SentRequestRowDto(
    @SerialName("to_user_id") val toUserId: String = "",
)

@Serializable
private data class FriendRequestRowDto(
    val id: String = "",
    @SerialName("from_user_id") val fromUserId: String = "",
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
private data class CommunityUserRowDto(
    val id: String = "",
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/**
 * Server-backed friendships. Requests and friendships live in Supabase (`friend_requests`, `friends`) and are changed
 * only through the SECURITY DEFINER functions of 20261014_friends_chat_realtime.sql, so both sides always see the
 * same state. (An earlier version kept everything in SharedPreferences, which meant the other person never received
 * a request.)
 */
class SupabaseFriendRepository(
    private val supabase: SupabaseClient?,
    @Suppress("UNUSED_PARAMETER") context: Context? = null,
) : FriendRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private fun requireClient(): SupabaseClient = supabase ?: error("Supabase לא מוגדר")

    private fun currentUserId(client: SupabaseClient): String =
        client.auth.currentUserOrNull()?.id ?: error("המשתמש אינו מחובר")

    private fun nameOf(p: UserProfileDto): String =
        p.displayName.ifBlank { p.fullName.ifBlank { p.username ?: "משתמש" } }

    override suspend fun getFriends(): Result<List<FriendUser>> = runCatching {
        val client = requireClient()
        val me = currentUserId(client)

        val friendIds = client.from("friends")
            .select { filter { eq("user_id", me) } }
            .decodeList<FriendRowDto>()
            .map { it.friendId }
            .distinct()
        if (friendIds.isEmpty()) return@runCatching emptyList()

        val profiles = client.from("profiles")
            .select { filter { isIn("id", friendIds) } }
            .decodeList<UserProfileDto>()

        val treeCounts = runCatching {
            client.from("families")
                .select { filter { isIn("owner_id", friendIds) } }
                .decodeList<FamilyDto>()
                .groupingBy { it.ownerId }
                .eachCount()
        }.getOrDefault(emptyMap())

        profiles.map { p ->
            FriendUser(
                id = p.id,
                username = p.username,
                displayName = nameOf(p),
                avatarUrl = p.avatarUrl,
                isOnline = false,
                lastSeenText = "חבר באפליקציה",
                isPrivateProfile = p.isPrivate,
                treeCount = treeCounts[p.id] ?: 0,
            )
        }.sortedBy { it.displayName }
    }.onFailure { Log.e(TAG, "getFriends failed", it) }

    override suspend fun getFriendSuggestions(): Result<List<FriendSuggestion>> = runCatching {
        val client = requireClient()
        val me = currentUserId(client)

        val friendIds = client.from("friends")
            .select { filter { eq("user_id", me) } }
            .decodeList<FriendRowDto>().map { it.friendId }.toSet()
        val requestedIds = runCatching {
            client.from("friend_requests")
                .select { filter { eq("from_user_id", me); eq("status", "PENDING") } }
                .decodeList<SentRequestRowDto>().map { it.toUserId }.toSet()
        }.getOrDefault(emptySet())

        // 1. Try dedicated SECURITY DEFINER RPC first (guaranteed to return users)
        val rpcSuggestions = runCatching {
            client.postgrest.rpc("get_community_users")
                .decodeList<CommunityUserRowDto>()
                .filter { it.id !in friendIds && it.id != me }
                .take(30)
                .map { u ->
                    FriendSuggestion(
                        id = u.id,
                        username = u.username,
                        displayName = u.displayName?.ifBlank { null } ?: u.username ?: "משתמש",
                        avatarUrl = u.avatarUrl,
                        reason = if (u.id in requestedIds) "בקשת חברות ממתינה" else "משתמש באפליקציית שורשים",
                    )
                }
        }.getOrNull()

        if (!rpcSuggestions.isNullOrEmpty()) {
            return@runCatching rpcSuggestions
        }

        // 2. Direct profiles table fallback
        client.from("profiles")
            .select {
                filter { neq("id", me) }
                limit(100)
            }
            .decodeList<UserProfileDto>()
            .filter { it.id !in friendIds && (!it.isPrivate || !it.username.isNullOrBlank()) }
            .take(30)
            .map { p ->
                FriendSuggestion(
                    id = p.id,
                    username = p.username,
                    displayName = nameOf(p),
                    avatarUrl = p.avatarUrl,
                    reason = if (p.id in requestedIds) "בקשת חברות ממתינה" else "משתמש באפליקציית שורשים",
                )
            }
    }.onFailure { Log.e(TAG, "getFriendSuggestions failed", it) }

    override suspend fun getPendingRequests(): Result<List<FriendRequest>> = runCatching {
        val client = requireClient()
        client.postgrest.rpc("get_my_friend_requests")
            .decodeList<FriendRequestRowDto>()
            .map { r ->
                FriendRequest(
                    id = r.id,
                    fromUserId = r.fromUserId,
                    fromUsername = r.username,
                    fromDisplayName = r.displayName?.ifBlank { null }
                        ?: r.fullName?.ifBlank { null }
                        ?: r.username
                        ?: "משתמש",
                    fromAvatarUrl = r.avatarUrl,
                    createdAt = r.createdAt.orEmpty(),
                )
            }
    }.onFailure { Log.e(TAG, "getPendingRequests failed", it) }

    override suspend fun sendFriendRequest(targetUserIdOrUsername: String): Result<Unit> = runCatching {
        val client = requireClient()
        val raw = client.postgrest.rpc(
            function = "send_friend_request",
            parameters = buildJsonObject { put("p_target", targetUserIdOrUsername.trim()) },
        ).data
        val obj = json.parseToJsonElement(raw).jsonObject
        if (obj["ok"]?.jsonPrimitive?.booleanOrNull != true) {
            when (obj["error"]?.jsonPrimitive?.contentOrNull) {
                "user_not_found" -> error("המשתמש לא נמצא")
                "cannot_add_self" -> error("לא ניתן להוסיף את עצמך כחבר")
                "already_friends" -> error("אתם כבר חברים")
                "already_pending" -> error("כבר שלחת בקשת חברות למשתמש זה")
                "not_authenticated" -> error("יש להתחבר מחדש")
                else -> error("שליחת הבקשה נכשלה")
            }
        }
        Unit
    }.onFailure { Log.e(TAG, "sendFriendRequest failed", it) }

    override suspend fun respondToFriendRequest(requestId: String, accept: Boolean): Result<Unit> = runCatching {
        val client = requireClient()
        val raw = client.postgrest.rpc(
            function = "respond_friend_request",
            parameters = buildJsonObject {
                put("p_request_id", requestId)
                put("p_accept", accept)
            },
        ).data
        val obj = json.parseToJsonElement(raw).jsonObject
        if (obj["ok"]?.jsonPrimitive?.booleanOrNull != true) {
            error("הבקשה לא נמצאה או שכבר טופלה")
        }
        Unit
    }.onFailure { Log.e(TAG, "respondToFriendRequest failed", it) }

    override suspend fun removeFriend(friendUserId: String): Result<Unit> = runCatching {
        val client = requireClient()
        client.postgrest.rpc(
            function = "remove_friend",
            parameters = buildJsonObject { put("p_friend_id", friendUserId) },
        )
        Unit
    }.onFailure { Log.e(TAG, "removeFriend failed", it) }

    override suspend fun getFriendTrees(friendUserId: String): Result<List<Family>> = runCatching {
        val client = requireClient()
        val profile = runCatching {
            client.from("profiles")
                .select { filter { eq("id", friendUserId) } }
                .decodeSingleOrNull<UserProfileDto>()
        }.getOrNull()

        // A private profile hides its trees.
        if (profile?.isPrivate == true) return@runCatching emptyList()

        client.from("families")
            .select { filter { eq("owner_id", friendUserId) } }
            .decodeList<FamilyDto>()
            .map { dto ->
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
            // Only trees the friend made public are visible to others.
            .filter { it.isPublic }
    }.onFailure { Log.e(TAG, "getFriendTrees failed", it) }
}
