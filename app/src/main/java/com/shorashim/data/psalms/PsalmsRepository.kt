package com.shorashim.data.psalms

import com.shorashim.core.model.TehillimCatalogStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class PsalmChapter(
    val chapter: Int,
    val hebrewTitle: String,
    val verses: List<String>,
)

interface PsalmsRepository {
    fun observeCatalogStatus(): Flow<TehillimCatalogStatus>
    fun getAllChapters(): List<PsalmChapter>
    fun getChapter(number: Int): PsalmChapter?
    fun getMemorialChapters(): List<PsalmChapter>

    companion object {
        /**
         * Required traditional memorial chapters in their exact prescribed order:
         * 33 (ל"ג), 16 (ט"ז), 17 (י"ז), 72 (ע"ב), 91 (צ"א), 104 (ק"ד), 130 (ק"ל).
         */
        val MEMORIAL_CHAPTER_NUMBERS = listOf(33, 16, 17, 72, 91, 104, 130)
    }
}

class UnavailablePsalmsRepository : PsalmsRepository {
    override fun observeCatalogStatus(): Flow<TehillimCatalogStatus> =
        kotlinx.coroutines.flow.flowOf(TehillimCatalogStatus(verifiedTextAvailable = false, chapterCount = 0))

    override fun getAllChapters(): List<PsalmChapter> = emptyList()

    override fun getChapter(number: Int): PsalmChapter? = null

    override fun getMemorialChapters(): List<PsalmChapter> = emptyList()
}

