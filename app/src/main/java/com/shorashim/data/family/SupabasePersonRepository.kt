package com.shorashim.data.family

import android.content.Context
import android.util.Log
import com.shorashim.core.model.Person
import com.shorashim.core.model.PersonDto
import com.shorashim.core.model.PersonUpdateDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabasePersonRepository(
    private val client: SupabaseClient?,
    private val context: Context? = null,
) : PersonRepository {

    private fun saveLocalLink(personId: String, userId: String?) {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("shorashim_person_links_prefs", Context.MODE_PRIVATE)
        if (userId.isNullOrBlank()) {
            prefs.edit().remove("link_$personId").apply()
        } else {
            prefs.edit().putString("link_$personId", userId).apply()
        }
    }

    private fun getLocalLink(personId: String): String? {
        val ctx = context ?: return null
        val prefs = ctx.getSharedPreferences("shorashim_person_links_prefs", Context.MODE_PRIVATE)
        return prefs.getString("link_$personId", null)?.ifBlank { null }
    }

    private fun getCurrentUserId(): String? {
        return client?.auth?.currentUserOrNull()?.id
    }

    override suspend fun getPeople(familyId: String): Result<List<Person>> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            val dtos = supabase.from("people")
                .select {
                    filter {
                        eq("family_id", familyId)
                    }
                }
                .decodeList<PersonDto>()

            dtos.map { it.toDomain() }
        }.onFailure { e ->
            Log.e("PersonRepo", "Failed to fetch people for family $familyId", e)
        }
    }

    override suspend fun getAllAccessiblePeople(): Result<List<Person>> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            val dtos = supabase.from("people")
                .select()
                .decodeList<PersonDto>()

            dtos.map { it.toDomain() }
        }.onFailure { e ->
            Log.e("PersonRepo", "Failed to fetch all accessible people", e)
        }
    }

    override suspend fun getPerson(personId: String): Result<Person> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            val dto = supabase.from("people")
                .select {
                    filter {
                        eq("id", personId)
                    }
                }
                .decodeSingle<PersonDto>()

            dto.toDomain()
        }
    }

    override suspend fun createPerson(person: Person): Result<Person> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        val userId = getCurrentUserId()

        return runCatching {
            val insertDto = com.shorashim.core.model.PersonInsertDto(
                familyId = person.familyId,
                firstName = person.firstName.trim(),
                lastName = person.lastName?.trim()?.ifBlank { null },
                additionalNames = person.additionalNames?.trim()?.ifBlank { null },
                photoPath = person.photoPath,
                lifeStatus = person.lifeStatus,
                birthDate = person.birthDate,
                birthDatePrecision = person.birthDatePrecision,
                deathDate = person.deathDate,
                deathDatePrecision = person.deathDatePrecision,
                biography = person.biography?.trim()?.ifBlank { null },
                canvasX = person.canvasX,
                canvasY = person.canvasY,
                createdBy = userId,
                linkedUserId = person.linkedUserId,
            )

            val created = supabase.from("people").insert(insertDto) {
                select()
            }.decodeSingle<PersonDto>()

            created.toDomain()
        }.onFailure { e ->
            Log.e("PersonRepo", "Failed to create person", e)
        }
    }

    override suspend fun updatePerson(person: Person): Result<Person> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            val dto = PersonUpdateDto(
                firstName = person.firstName.trim(),
                lastName = person.lastName?.trim()?.ifBlank { null },
                additionalNames = person.additionalNames?.trim()?.ifBlank { null },
                photoPath = person.photoPath,
                lifeStatus = person.lifeStatus,
                birthDate = person.birthDate,
                birthDatePrecision = person.birthDatePrecision,
                deathDate = person.deathDate,
                deathDatePrecision = person.deathDatePrecision,
                biography = person.biography?.trim()?.ifBlank { null },
                canvasX = person.canvasX,
                canvasY = person.canvasY,
                linkedUserId = person.linkedUserId,
            )

            val updated = supabase.from("people").update(dto) {
                filter { eq("id", person.id) }
                select()
            }.decodeSingle<PersonDto>()

            updated.toDomain()
        }
    }

    override suspend fun updatePersonPosition(
        personId: String,
        canvasX: Double,
        canvasY: Double,
    ): Result<Unit> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            supabase.from("people").update(
                mapOf(
                    "canvas_x" to canvasX,
                    "canvas_y" to canvasY,
                )
            ) {
                filter { eq("id", personId) }
            }
            Unit
        }
    }

    override suspend fun linkUserToPerson(personId: String, userId: String?): Result<Unit> {
        saveLocalLink(personId, userId)
        val supabase = client ?: return Result.success(Unit)
        return runCatching {
            supabase.from("people").update(
                kotlinx.serialization.json.buildJsonObject {
                    if (userId == null) {
                        put("linked_user_id", kotlinx.serialization.json.JsonNull)
                    } else {
                        put("linked_user_id", userId)
                    }
                }
            ) {
                filter { eq("id", personId) }
            }
            Unit
        }.recover {
            Log.w("PersonRepo", "Supabase update of linked_user_id failed (remote schema may need migration), preserved locally: ${it.message}")
            Unit
        }
    }

    override suspend fun getMutualFamilyConnections(targetUserId: String): Result<List<com.shorashim.core.model.MutualFamilyConnection>> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            try {
                supabase.postgrest.rpc(
                    "get_mutual_family_connections",
                    kotlinx.serialization.json.buildJsonObject {
                        put("p_target_user_id", targetUserId)
                    }
                ).decodeList<com.shorashim.core.model.MutualFamilyConnection>()
            } catch (t: Throwable) {
                Log.w("PersonRepo", "RPC get_mutual_family_connections failed, falling back to accessible people check: ${t.message}")
                val peopleList = getAllAccessiblePeople().getOrDefault(emptyList())
                val matched = peopleList.filter { it.linkedUserId == targetUserId }
                if (matched.isEmpty()) emptyList() else {
                    val famIds = matched.map { it.familyId }.distinct()
                    val families = try {
                        supabase.from("families").select {
                            filter { isIn("id", famIds) }
                        }.decodeList<com.shorashim.core.model.FamilyDto>().associateBy { it.id }
                    } catch (_: Throwable) {
                        emptyMap()
                    }

                    matched.map { p ->
                        val famName = families[p.familyId]?.name ?: "עץ משפחה"
                        com.shorashim.core.model.MutualFamilyConnection(
                            familyId = p.familyId,
                            familyName = famName,
                            personId = p.id,
                            personName = p.fullName,
                            role = "חבר בעץ",
                        )
                    }
                }
            }
        }.recover {
            emptyList()
        }
    }

    override suspend fun deletePerson(personId: String): Result<Unit> {
        val supabase = client ?: return Result.failure(IllegalStateException("Supabase client not initialized"))
        return runCatching {
            supabase.from("people").delete {
                filter { eq("id", personId) }
            }
            Unit
        }
    }

    override suspend fun searchPeople(familyId: String, query: String): Result<List<Person>> {
        val all = getPeople(familyId)
        return all.map { list ->
            if (query.isBlank()) {
                list
            } else {
                val q = query.trim().lowercase()
                list.filter { p ->
                    p.firstName.lowercase().contains(q) ||
                        (p.lastName?.lowercase()?.contains(q) == true) ||
                        (p.additionalNames?.lowercase()?.contains(q) == true)
                }
            }
        }
    }

    private fun PersonDto.toDomain(): Person {
        val localLinked = getLocalLink(id)
        val effectiveLinkedId = if (!linkedUserId.isNullOrBlank()) linkedUserId else localLinked
        return Person(
            id = id,
            familyId = familyId,
            firstName = firstName,
            lastName = lastName,
            additionalNames = additionalNames,
            photoPath = photoPath,
            lifeStatus = lifeStatus,
            birthDate = birthDate,
            birthDatePrecision = birthDatePrecision,
            deathDate = deathDate,
            deathDatePrecision = deathDatePrecision,
            biography = biography,
            canvasX = canvasX,
            canvasY = canvasY,
            linkedUserId = effectiveLinkedId,
            createdAt = createdAt,
        )
    }
}
