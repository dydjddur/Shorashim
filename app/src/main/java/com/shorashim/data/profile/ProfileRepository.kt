package com.shorashim.data.profile

import com.shorashim.core.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): Result<UserProfile>
    suspend fun updateProfile(fullName: String, displayName: String): Result<UserProfile>
    suspend fun searchUserByUsername(username: String): Result<UserProfile?>
    suspend fun setUsername(username: String): Result<Unit>
}
