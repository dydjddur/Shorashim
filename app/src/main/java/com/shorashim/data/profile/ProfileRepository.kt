package com.shorashim.data.profile

import com.shorashim.core.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): Result<UserProfile>
    suspend fun updateProfile(fullName: String, displayName: String): Result<UserProfile> =
        updateProfile(fullName, displayName, null, false, null)
    suspend fun updateProfile(
        fullName: String,
        displayName: String,
        avatarUrl: String?,
        isPrivate: Boolean,
        bio: String?,
    ): Result<UserProfile>
    suspend fun setPrivateAccountMode(isPrivate: Boolean): Result<Unit>
    suspend fun searchUserByUsername(username: String): Result<UserProfile?>
    suspend fun setUsername(username: String): Result<Unit>
}
