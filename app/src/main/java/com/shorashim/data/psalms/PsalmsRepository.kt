package com.shorashim.data.psalms

import com.shorashim.core.model.TehillimCatalogStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PsalmsRepository {
    fun observeCatalogStatus(): Flow<TehillimCatalogStatus>
}

/**
 * Phase 1 stub. Verified Tehillim text is not packaged yet (Phase 9).
 */
class UnavailablePsalmsRepository : PsalmsRepository {
    private val status = MutableStateFlow(
        TehillimCatalogStatus(verifiedTextAvailable = false, chapterCount = 0),
    )

    override fun observeCatalogStatus(): Flow<TehillimCatalogStatus> = status.asStateFlow()
}
