package com.shorashim.data.memorials

import android.util.Log
import com.shorashim.core.model.MemorialEvent
import com.shorashim.core.model.MemorialEventDto
import com.shorashim.core.model.MemorialEventInsertDto
import com.shorashim.core.model.MemorialSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseMemorialRepository(
    private val supabase: SupabaseClient?,
) : MemorialRepository {

    private val summaryFlow = MutableStateFlow(MemorialSummary())

    override fun observeMemorialSummary(): Flow<MemorialSummary> = summaryFlow.asStateFlow()

    private fun getCurrentUserId(): String? = supabase?.auth?.currentUserOrNull()?.id

    override suspend fun getMemorialEvents(familyId: String?): Result<List<MemorialEvent>> {
        val client = supabase ?: return Result.success(emptyList())

        return runCatching {
            val dtoList = if (familyId != null) {
                client.from("memorial_events")
                    .select {
                        filter {
                            eq("family_id", familyId)
                        }
                    }
                    .decodeList<MemorialEventDto>()
            } else {
                client.from("memorial_events")
                    .select()
                    .decodeList<MemorialEventDto>()
            }

            dtoList.map { dto ->
                MemorialEvent(
                    id = dto.id,
                    familyId = dto.familyId,
                    personId = dto.personId,
                    personName = dto.personName,
                    eventDate = dto.eventDate,
                    hebrewDate = dto.hebrewDate,
                    description = dto.description,
                    createdBy = dto.createdBy,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                )
            }.also { events ->
                summaryFlow.value = MemorialSummary(upcomingCount = events.size)
            }
        }.onFailure { e ->
            Log.e("MemorialRepo", "Failed to get memorial events: ${e.message}", e)
        }
    }

    override suspend fun createMemorialEvent(insertDto: MemorialEventInsertDto): Result<MemorialEvent> {
        val client = supabase ?: return Result.failure(IllegalStateException("Supabase not configured"))
        val userId = getCurrentUserId()

        return runCatching {
            val payload = insertDto.copy(createdBy = insertDto.createdBy ?: userId)
            val created = client.from("memorial_events").insert(payload) {
                select()
            }.decodeSingle<MemorialEventDto>()

            MemorialEvent(
                id = created.id,
                familyId = created.familyId,
                personId = created.personId,
                personName = created.personName,
                eventDate = created.eventDate,
                hebrewDate = created.hebrewDate,
                description = created.description,
                createdBy = created.createdBy,
                createdAt = created.createdAt,
                updatedAt = created.updatedAt,
            )
        }.onFailure { e ->
            Log.e("MemorialRepo", "Failed to create memorial event: ${e.message}", e)
        }
    }

    override suspend fun updateMemorialEvent(
        id: String,
        insertDto: MemorialEventInsertDto,
    ): Result<MemorialEvent> {
        val client = supabase ?: return Result.failure(IllegalStateException("Supabase not configured"))

        return runCatching {
            val updated = client.from("memorial_events").update(insertDto) {
                filter {
                    eq("id", id)
                }
                select()
            }.decodeSingle<MemorialEventDto>()

            MemorialEvent(
                id = updated.id,
                familyId = updated.familyId,
                personId = updated.personId,
                personName = updated.personName,
                eventDate = updated.eventDate,
                hebrewDate = updated.hebrewDate,
                description = updated.description,
                createdBy = updated.createdBy,
                createdAt = updated.createdAt,
                updatedAt = updated.updatedAt,
            )
        }.onFailure { e ->
            Log.e("MemorialRepo", "Failed to update memorial event: ${e.message}", e)
        }
    }

    override suspend fun deleteMemorialEvent(id: String): Result<Unit> {
        val client = supabase ?: return Result.failure(IllegalStateException("Supabase not configured"))

        return runCatching {
            client.from("memorial_events").delete {
                filter {
                    eq("id", id)
                }
            }
            Unit
        }.onFailure { e ->
            Log.e("MemorialRepo", "Failed to delete memorial event: ${e.message}", e)
        }
    }
}
