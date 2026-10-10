package com.shorashim.data.memorials

import com.shorashim.core.model.MemorialEvent
import com.shorashim.core.model.MemorialEventInsertDto
import com.shorashim.core.model.MemorialSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface MemorialRepository {
    fun observeMemorialSummary(): Flow<MemorialSummary>
    suspend fun getMemorialEvents(familyId: String? = null): Result<List<MemorialEvent>>
    suspend fun createMemorialEvent(insertDto: MemorialEventInsertDto): Result<MemorialEvent>
    suspend fun updateMemorialEvent(id: String, insertDto: MemorialEventInsertDto): Result<MemorialEvent>
    suspend fun deleteMemorialEvent(id: String): Result<Unit>
}

class InMemoryMemorialRepository : MemorialRepository {
    private val summary = MutableStateFlow(MemorialSummary())
    private val events = mutableListOf<MemorialEvent>()

    override fun observeMemorialSummary(): Flow<MemorialSummary> = summary.asStateFlow()

    override suspend fun getMemorialEvents(familyId: String?): Result<List<MemorialEvent>> {
        return Result.success(
            if (familyId == null) events.toList()
            else events.filter { it.familyId == familyId }
        )
    }

    override suspend fun createMemorialEvent(insertDto: MemorialEventInsertDto): Result<MemorialEvent> {
        val created = MemorialEvent(
            id = java.util.UUID.randomUUID().toString(),
            familyId = insertDto.familyId,
            personId = insertDto.personId,
            personName = insertDto.personName,
            eventDate = insertDto.eventDate,
            hebrewDate = insertDto.hebrewDate,
            description = insertDto.description,
            createdBy = insertDto.createdBy,
        )
        events.add(created)
        return Result.success(created)
    }

    override suspend fun updateMemorialEvent(id: String, insertDto: MemorialEventInsertDto): Result<MemorialEvent> {
        val index = events.indexOfFirst { it.id == id }
        if (index < 0) return Result.failure(NoSuchElementException("Memorial event not found"))
        val updated = events[index].copy(
            personName = insertDto.personName,
            eventDate = insertDto.eventDate,
            hebrewDate = insertDto.hebrewDate,
            description = insertDto.description,
        )
        events[index] = updated
        return Result.success(updated)
    }

    override suspend fun deleteMemorialEvent(id: String): Result<Unit> {
        events.removeAll { it.id == id }
        return Result.success(Unit)
    }
}

