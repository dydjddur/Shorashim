package com.shorashim.data.memorials

import com.shorashim.core.model.MemorialSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface MemorialRepository {
    fun observeMemorialSummary(): Flow<MemorialSummary>
}

class InMemoryMemorialRepository : MemorialRepository {
    private val summary = MutableStateFlow(MemorialSummary())

    override fun observeMemorialSummary(): Flow<MemorialSummary> = summary.asStateFlow()
}
