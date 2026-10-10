package com.shorashim.data.family

import com.shorashim.core.model.FamilySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface FamilyRepository {
    fun observeFamilySummary(): Flow<FamilySummary>
}

/**
 * Phase 1 stub: no persistence and no Firebase.
 * Always empty so the UI cannot pretend a family tree exists.
 */
class InMemoryFamilyRepository : FamilyRepository {
    private val summary = MutableStateFlow(FamilySummary())

    override fun observeFamilySummary(): Flow<FamilySummary> = summary.asStateFlow()
}
