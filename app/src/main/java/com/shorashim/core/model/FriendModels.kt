package com.shorashim.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendUser(
    val id: String,
    val username: String? = null,
    val displayName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val isOnline: Boolean = true,
    val lastSeenText: String = "מחובר עכשיו",
    val isPrivateProfile: Boolean = false,
    val treeCount: Int = 0,
)

@Serializable
data class FriendSuggestion(
    val id: String,
    val username: String? = null,
    val displayName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val reason: String,
)

@Serializable
data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromUsername: String? = null,
    val fromDisplayName: String,
    val fromAvatarUrl: String? = null,
    val createdAt: String = "",
)
