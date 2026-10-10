package com.shorashim.data.psalms

import android.content.Context
import android.util.Log
import com.shorashim.core.model.TehillimCatalogStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class LocalPsalmsRepository(
    private val context: Context,
) : PsalmsRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val chapters: List<PsalmChapter> by lazy { loadChapters() }

    private val catalogStatusFlow = MutableStateFlow(
        TehillimCatalogStatus(
            verifiedTextAvailable = true,
            chapterCount = 150,
        )
    )

    override fun observeCatalogStatus(): Flow<TehillimCatalogStatus> = catalogStatusFlow.asStateFlow()

    override fun getAllChapters(): List<PsalmChapter> = chapters

    override fun getChapter(number: Int): PsalmChapter? = chapters.find { it.chapter == number }

    override fun getMemorialChapters(): List<PsalmChapter> {
        val chapterMap = chapters.associateBy { it.chapter }
        return PsalmsRepository.MEMORIAL_CHAPTER_NUMBERS.mapNotNull { chapterMap[it] }
    }

    private fun loadChapters(): List<PsalmChapter> {
        return try {
            context.assets.open("tehillim_150.json").use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }.removePrefix("\uFEFF")
                json.decodeFromString<List<PsalmChapter>>(content)
            }
        } catch (e: Throwable) {
            Log.e("LocalPsalmsRepo", "Failed to load tehillim_150.json from assets", e)
            emptyList()
        }
    }
}
