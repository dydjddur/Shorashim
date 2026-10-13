package com.shorashim.data.friends

import com.shorashim.core.model.Family
import com.shorashim.core.model.FriendRequest
import com.shorashim.core.model.FriendSuggestion
import com.shorashim.core.model.FriendUser

interface FriendRepository {
    suspend fun getFriends(): Result<List<FriendUser>>
    suspend fun getFriendSuggestions(): Result<List<FriendSuggestion>>
    suspend fun getPendingRequests(): Result<List<FriendRequest>>
    suspend fun sendFriendRequest(targetUserIdOrUsername: String): Result<Unit>
    suspend fun respondToFriendRequest(requestId: String, accept: Boolean): Result<Unit>
    suspend fun removeFriend(friendUserId: String): Result<Unit>
    suspend fun getFriendTrees(friendUserId: String): Result<List<Family>>
}
