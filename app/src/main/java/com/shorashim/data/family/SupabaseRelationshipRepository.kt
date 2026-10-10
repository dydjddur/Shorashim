package com.shorashim.data.family

import android.util.Log
import com.shorashim.core.model.Relationship
import com.shorashim.core.model.RelationshipDto
import com.shorashim.core.model.RelationshipType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class SupabaseRelationshipRepository(
    private val client: SupabaseClient?,
) : RelationshipRepository {

    private fun getCurrentUserId(): String? {
        return client?.auth?.currentUserOrNull()?.id
    }

    override suspend fun getRelationships(familyId: String): Result<List<Relationship>> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            val dtos = supabase.from("relationships")
                .select {
                    filter {
                        eq("family_id", familyId)
                    }
                }
                .decodeList<RelationshipDto>()

            dtos.map { it.toDomain() }
        }.onFailure { e ->
            Log.e("RelRepo", "Failed to get relationships for family $familyId", e)
        }
    }

    override suspend fun createRelationship(
        familyId: String,
        personId: String,
        relatedPersonId: String,
        type: RelationshipType,
    ): Result<Relationship> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        val userId = getCurrentUserId()

        return runCatching {
            val dto = RelationshipDto(
                familyId = familyId,
                personId = personId,
                relatedPersonId = relatedPersonId,
                relationshipType = type,
                createdBy = userId,
            )

            val created = supabase.from("relationships").insert(dto) {
                select()
            }.decodeSingle<RelationshipDto>()

            created.toDomain()
        }.onFailure { e ->
            Log.e("RelRepo", "Failed to create relationship", e)
        }
    }

    override suspend fun deleteRelationship(relationshipId: String): Result<Unit> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            supabase.from("relationships").delete {
                filter { eq("id", relationshipId) }
            }
            Unit
        }
    }

    private fun RelationshipDto.toDomain(): Relationship = Relationship(
        id = id,
        familyId = familyId,
        personId = personId,
        relatedPersonId = relatedPersonId,
        relationshipType = relationshipType,
        createdAt = createdAt,
    )
}
