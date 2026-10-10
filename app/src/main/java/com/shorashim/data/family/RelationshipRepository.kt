package com.shorashim.data.family

import com.shorashim.core.model.Relationship
import com.shorashim.core.model.RelationshipType

interface RelationshipRepository {
    suspend fun getRelationships(familyId: String): Result<List<Relationship>>
    suspend fun createRelationship(
        familyId: String,
        personId: String,
        relatedPersonId: String,
        type: RelationshipType,
    ): Result<Relationship>
    suspend fun deleteRelationship(relationshipId: String): Result<Unit>
}
