package com.shorashim.data.family

import android.util.Log
import com.shorashim.core.model.Family
import com.shorashim.core.model.FamilyCreateRequest
import com.shorashim.core.model.FamilyDto
import com.shorashim.core.model.FamilyMember
import com.shorashim.core.model.FamilyMemberDto
import com.shorashim.core.model.FamilyStatus
import com.shorashim.core.model.FamilySummary
import com.shorashim.core.model.MembershipRole
import com.shorashim.core.model.PersonDto
import com.shorashim.core.model.PublicFamilyTree
import com.shorashim.core.model.PublicFamilyTreeDto
import com.shorashim.core.model.UserProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseFamilyRepository(
    private val client: SupabaseClient?,
) : FamilyRepository {

    private val _familySummary = MutableStateFlow(FamilySummary())
    override fun observeFamilySummary(): Flow<FamilySummary> = _familySummary.asStateFlow()

    private fun getCurrentUserId(): String? {
        return client?.auth?.currentUserOrNull()?.id
    }

    private fun getCurrentUserEmail(): String? {
        return client?.auth?.currentUserOrNull()?.email
    }

    override suspend fun getMyFamilies(): Result<List<Family>> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not authenticated"))

        return runCatching {
            // Query member roles for current user
            val memberRoles = runCatching {
                supabase.from("family_members")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<FamilyMemberDto>()
                    .associateBy({ it.familyId }, { it.role })
            }.getOrDefault(emptyMap())

            // Fetch families from server
            val dtos = supabase.from("families")
                .select()
                .decodeList<FamilyDto>()

            // Filter strictly: only trees created by the user OR joined via link/invitation
            val myDtos = dtos.filter { dto ->
                dto.ownerId == userId || memberRoles.containsKey(dto.id)
            }

            val families = myDtos.map { dto ->
                val peopleCount = runCatching {
                    supabase.from("people")
                        .select {
                            filter {
                                eq("family_id", dto.id)
                            }
                        }
                        .decodeList<com.shorashim.core.model.PersonDto>()
                        .size
                }.getOrDefault(0)

                val userRole = if (dto.ownerId == userId) {
                    MembershipRole.OWNER
                } else {
                    memberRoles[dto.id] ?: MembershipRole.VIEWER
                }

                Family(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    ownerId = dto.ownerId,
                    status = dto.status,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    personCount = peopleCount,
                    currentUserRole = userRole,
                )
            }

            // Update summary flow for first family if available
            val first = families.firstOrNull()
            if (first != null) {
                _familySummary.value = FamilySummary(
                    familyId = first.id,
                    name = first.name,
                    personCount = first.personCount,
                )
            } else {
                _familySummary.value = FamilySummary()
            }

            families
        }.onFailure { e ->
            Log.e("FamilyRepo", "Failed to fetch families", e)
        }
    }

    override suspend fun getFamily(familyId: String): Result<Family> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        val userId = getCurrentUserId()

        return runCatching {
            val dto = supabase.from("families")
                .select {
                    filter {
                        eq("id", familyId)
                    }
                }
                .decodeSingle<FamilyDto>()

            val count = runCatching {
                supabase.from("people")
                    .select { filter { eq("family_id", dto.id) } }
                    .decodeList<com.shorashim.core.model.PersonDto>()
                    .size
            }.getOrDefault(0)

            val role = if (userId != null && dto.ownerId == userId) {
                MembershipRole.OWNER
            } else if (userId != null) {
                runCatching {
                    supabase.from("family_members")
                        .select {
                            filter {
                                eq("family_id", familyId)
                                eq("user_id", userId)
                            }
                        }
                        .decodeSingle<FamilyMemberDto>()
                        .role
                }.getOrDefault(MembershipRole.VIEWER)
            } else {
                MembershipRole.VIEWER
            }

            Family(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                ownerId = dto.ownerId,
                status = dto.status,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt,
                personCount = count,
                currentUserRole = role,
            )
        }
    }

    override suspend fun createFamily(name: String, description: String?): Result<Family> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not authenticated"))

        return runCatching {
            val dto = try {
                supabase.postgrest.rpc(
                    "create_family",
                    buildJsonObject {
                        put("p_name", name.trim())
                    },
                ).decodeSingle<FamilyDto>()
            } catch (t: Throwable) {
                Log.w("FamilyRepo", "RPC create_family failed, falling back to direct table insert: ${t.message}")
                val insertReq = FamilyCreateRequest(
                    name = name.trim(),
                    ownerId = userId,
                    status = FamilyStatus.PUBLISHED,
                    description = description?.trim()?.ifBlank { null },
                )
                supabase.from("families").insert(insertReq) {
                    select()
                }.decodeSingle<FamilyDto>()
            }

            val family = Family(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                ownerId = dto.ownerId,
                status = dto.status,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt,
                personCount = 0,
                currentUserRole = MembershipRole.OWNER,
            )

            _familySummary.value = FamilySummary(
                familyId = family.id,
                name = family.name,
                personCount = 0,
            )

            family
        }.onFailure { e ->
            Log.e("FamilyRepo", "Failed to create family", e)
        }
    }

    override suspend fun renameFamily(familyId: String, newName: String): Result<Family> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            val dto = supabase.from("families").update(
                buildJsonObject { put("name", newName.trim()) },
            ) {
                filter { eq("id", familyId) }
                select()
            }.decodeSingle<FamilyDto>()

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

    override suspend fun publishFamily(familyId: String): Result<Family> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            val dto = supabase.from("families").update(
                buildJsonObject { put("status", "PUBLISHED") },
            ) {
                filter { eq("id", familyId) }
                select()
            }.decodeSingle<FamilyDto>()

            Family(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                ownerId = dto.ownerId,
                status = FamilyStatus.PUBLISHED,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt,
            )
        }
    }

    /**
     * Hides the tree from the community. Uses the dedicated PRIVATE status so a later migration or a "draft" flow
     * can never silently flip it back to public; falls back to DRAFT on databases whose status CHECK constraint
     * does not know PRIVATE yet. Both are treated as private by [Family.isPrivate] and by the community RPC.
     */
    override suspend fun unpublishFamily(familyId: String): Result<Family> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        suspend fun setStatus(status: String): FamilyDto = supabase.from("families").update(
            buildJsonObject { put("status", status) },
        ) {
            filter { eq("id", familyId) }
            select()
        }.decodeSingle<FamilyDto>()

        return runCatching {
            val dto = try {
                setStatus("PRIVATE")
            } catch (t: Throwable) {
                Log.w("FamilyRepo", "PRIVATE status rejected (${t.message}); falling back to DRAFT")
                setStatus("DRAFT")
            }

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

    override suspend fun deleteFamily(familyId: String): Result<Unit> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            supabase.from("families").delete {
                filter { eq("id", familyId) }
            }
            if (_familySummary.value.familyId == familyId) {
                _familySummary.value = FamilySummary()
            }
            Unit
        }
    }

    override suspend fun getFamilyMembers(familyId: String): Result<List<FamilyMember>> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        val currentUserId = getCurrentUserId()

        return runCatching {
            val dtos = supabase.from("family_members")
                .select {
                    filter {
                        eq("family_id", familyId)
                    }
                }
                .decodeList<FamilyMemberDto>()

            val userIds = dtos.map { it.userId }
            val profilesMap = try {
                if (userIds.isNotEmpty()) {
                    supabase.from("profiles")
                        .select {
                            filter {
                                isIn("id", userIds)
                            }
                        }
                        .decodeList<UserProfileDto>()
                        .associateBy { it.id }
                } else emptyMap()
            } catch (t: Throwable) {
                Log.w("FamilyRepo", "Could not fetch member profiles: ${t.message}")
                emptyMap()
            }

            dtos.map { dto ->
                val profile = profilesMap[dto.userId]
                val disp = profile?.displayName?.ifBlank { profile.fullName }?.ifBlank { null }
                    ?: profile?.fullName?.ifBlank { null }
                    ?: profile?.username
                FamilyMember(
                    familyId = dto.familyId,
                    userId = dto.userId,
                    userEmail = if (dto.userId == currentUserId) getCurrentUserEmail() else null,
                    displayName = disp,
                    username = profile?.username,
                    avatarUrl = profile?.avatarUrl,
                    role = dto.role,
                    isCurrentUser = (dto.userId == currentUserId),
                    createdAt = dto.createdAt,
                )
            }
        }.onFailure { e ->
            Log.e("FamilyRepo", "Failed to get family members", e)
        }
    }

    override suspend fun updateMemberRole(
        familyId: String,
        userId: String,
        role: MembershipRole,
    ): Result<Unit> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            supabase.from("family_members").update(
                buildJsonObject { put("role", role.name) },
            ) {
                filter {
                    eq("family_id", familyId)
                    eq("user_id", userId)
                }
            }
            Unit
        }
    }

    override suspend fun inviteMember(
        familyId: String,
        email: String,
        role: MembershipRole,
    ): Result<Unit> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        val currentUserId = getCurrentUserId()

        return runCatching {
            // Note: In Supabase, if auth.users cannot be looked up by arbitrary email from client,
            // we insert an invitation or if user is found, add to family_members.
            val memberDto = FamilyMemberDto(
                familyId = familyId,
                userId = email.trim(), // Stored or mapped to target user
                role = role,
                invitedBy = currentUserId,
            )
            supabase.from("family_members").insert(memberDto)
            Unit
        }
    }

    override suspend fun removeMember(familyId: String, userId: String): Result<Unit> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            supabase.from("family_members").delete {
                filter {
                    eq("family_id", familyId)
                    eq("user_id", userId)
                }
            }
            Unit
        }
    }

    override suspend fun getPublicTrees(query: String?): Result<List<PublicFamilyTree>> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        val currentUserId = getCurrentUserId()

        return runCatching {
            // 1. Try high-performance RPC function first
            val rpcTrees = try {
                supabase.postgrest.rpc("get_public_trees_summary").decodeList<PublicFamilyTreeDto>()
            } catch (t: Throwable) {
                Log.w("FamilyRepo", "RPC get_public_trees_summary unavailable, falling back to manual query: ${t.message}")
                null
            }

            val list: List<PublicFamilyTree> = if (rpcTrees != null) {
                rpcTrees.map { dto ->
                    PublicFamilyTree(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        ownerId = dto.ownerId,
                        ownerName = dto.ownerName,
                        status = dto.status,
                        personCount = dto.personCount ?: 0,
                        surnames = dto.surnames ?: emptyList(),
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        isUserTree = currentUserId != null && dto.ownerId == currentUserId,
                    )
                }
            } else {
                // Fallback: Query families table for PUBLISHED families
                val famDtos = supabase.from("families")
                    .select {
                        filter {
                            eq("status", "PUBLISHED")
                        }
                    }
                    .decodeList<FamilyDto>()

                famDtos.map { famDto ->
                    val people = runCatching {
                        supabase.from("people")
                            .select {
                                filter {
                                    eq("family_id", famDto.id)
                                }
                            }
                            .decodeList<PersonDto>()
                    }.getOrDefault(emptyList())

                    val surnames = people
                        .mapNotNull { it.lastName?.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()

                    val ownerProfile = runCatching {
                        supabase.from("profiles")
                            .select {
                                filter {
                                    eq("id", famDto.ownerId)
                                }
                            }
                            .decodeSingle<UserProfileDto>()
                    }.getOrNull()

                    val ownerName = ownerProfile?.displayName?.ifBlank { null }
                        ?: ownerProfile?.fullName?.ifBlank { null }
                        ?: "יוצר העץ"

                    PublicFamilyTree(
                        id = famDto.id,
                        name = famDto.name,
                        description = famDto.description,
                        ownerId = famDto.ownerId,
                        ownerName = ownerName,
                        status = famDto.status,
                        personCount = people.size,
                        surnames = surnames,
                        createdAt = famDto.createdAt,
                        updatedAt = famDto.updatedAt,
                        isUserTree = currentUserId != null && famDto.ownerId == currentUserId,
                    )
                }
            }

            if (query.isNullOrBlank()) {
                list
            } else {
                val q = query.trim()
                list.filter { tree ->
                    tree.name.contains(q, ignoreCase = true) ||
                        tree.description?.contains(q, ignoreCase = true) == true ||
                        tree.surnames.any { it.contains(q, ignoreCase = true) } ||
                        tree.ownerName?.contains(q, ignoreCase = true) == true
                }
            }
        }.onFailure { e ->
            Log.e("FamilyRepo", "Failed to fetch public trees", e)
        }
    }

    override suspend fun getMySurnames(): Result<List<String>> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        val userId = getCurrentUserId() ?: return Result.success(emptyList())

        return runCatching {
            val myFamilies = getMyFamilies().getOrDefault(emptyList())
            val surnames = mutableSetOf<String>()

            for (family in myFamilies) {
                val people = runCatching {
                    supabase.from("people")
                        .select {
                            filter {
                                eq("family_id", family.id)
                            }
                        }
                        .decodeList<PersonDto>()
                }.getOrDefault(emptyList())

                for (person in people) {
                    val ln = person.lastName?.trim()
                    if (!ln.isNullOrBlank()) {
                        surnames.add(ln)
                    }
                }
            }

            surnames.sorted()
        }.onFailure { e ->
            Log.e("FamilyRepo", "Failed to get user surnames", e)
        }
    }
}
