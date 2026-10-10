package com.shorashim

import com.shorashim.data.psalms.PsalmChapter
import com.shorashim.data.psalms.PsalmsRepository
import com.shorashim.feature.tehillim.TehillimUiState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TehillimRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun tehillim150Json_containsExact150Chapters() {
        val assetFile = File("src/main/assets/tehillim_150.json")
        assertTrue("Assets file must exist", assetFile.exists())

        val chapters = json.decodeFromString<List<PsalmChapter>>(assetFile.readText())
        assertEquals("Must contain exactly 150 chapters", 150, chapters.size)

        // Verify chapter numbers 1 to 150 in sequence
        for (i in 1..150) {
            val chapter = chapters[i - 1]
            assertEquals(i, chapter.chapter)
            assertTrue("Chapter title must not be empty", chapter.hebrewTitle.isNotBlank())
            assertTrue("Chapter $i must have verses", chapter.verses.isNotEmpty())
        }

        // Verify chapter 1 has 6 verses
        assertEquals(6, chapters[0].verses.size)
        // Verify chapter 119 has 176 verses
        assertEquals(176, chapters[118].verses.size)
        // Verify chapter 150 has 6 verses
        assertEquals(6, chapters[149].verses.size)
    }

    @Test
    fun memorialChapters_exactPrescribedSevenChaptersInOrder() {
        val expected = listOf(33, 16, 17, 72, 91, 104, 130)
        assertEquals(expected, PsalmsRepository.MEMORIAL_CHAPTER_NUMBERS)

        val assetFile = File("src/main/assets/tehillim_150.json")
        val chapters = json.decodeFromString<List<PsalmChapter>>(assetFile.readText())
        val map = chapters.associateBy { it.chapter }

        val memorialChapters = PsalmsRepository.MEMORIAL_CHAPTER_NUMBERS.mapNotNull { map[it] }
        assertEquals(7, memorialChapters.size)
        assertEquals(listOf(33, 16, 17, 72, 91, 104, 130), memorialChapters.map { it.chapter })
    }

    @Test
    fun dedicationTemplate_formatsCorrectly() {
        val stateWithMother = TehillimUiState(
            deceasedName = "דוד כהן",
            motherName = "שרה",
        )
        assertEquals("לעילוי נשמת דוד כהן, דוד כהן בן/בת שרה", stateWithMother.dedicationText)

        val stateWithoutMother = TehillimUiState(
            deceasedName = "דוד כהן",
            motherName = "",
        )
        assertEquals("לעילוי נשמת דוד כהן", stateWithoutMother.dedicationText)

        val emptyState = TehillimUiState()
        assertEquals("לעילוי נשמת כל נפטרי עמו ישראל", emptyState.dedicationText)
    }
}
