package com.shorashim.data.family

import com.shorashim.core.model.Family
import com.shorashim.core.model.FamilyMember
import com.shorashim.core.model.FamilySummary
import com.shorashim.core.model.MembershipRole
import kotlinx.coroutines.flow.Flow

interface FamilyRepository {
    suspend fun getMyFamilies(): Result<List<Family>>
    suspend fun getFamily(familyId: String): Result<Family>
    suspend fun createFamily(name: String, description: String? = null): Result<Family>
    suspend fun renameFamily(familyId: String, newName: String): Result<Family>
    suspend fun publishFamily(familyId: String): Result<Family>
    suspend fun unpublishFamily(familyId: String): Result<Family>
    suspend fun deleteFamily(familyId: String): Result<Unit>

    suspend fun getFamilyMembers(familyId: String): Result<List<FamilyMember>>
    suspend fun updateMemberRole(familyId: String, userId: String, role: MembershipRole): Result<Unit>
    suspend fun inviteMember(familyId: String, email: String, role: MembershipRole): Result<Unit>
    suspend fun removeMember(familyId: String, userId: String): Result<Unit>

    /**
     * Backward-compatibility flow for Phase 1 & 2 bottom-bar family tree tab.
     */
    fun observeFamilySummary(): Flow<FamilySummary>
}
