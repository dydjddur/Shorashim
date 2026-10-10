package com.shorashim.data.family

import android.util.Log
import com.shorashim.core.model.Person
import com.shorashim.core.model.PersonDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class SupabasePersonRepository(
    private val client: SupabaseClient?,
) : PersonRepository {

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
            val dto = PersonDto(
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
            )

            val created = supabase.from("people").insert(dto) {
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
            val dto = PersonDto(
                id = person.id,
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

    private fun PersonDto.toDomain(): Person = Person(
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
        createdAt = createdAt,
    )
}
