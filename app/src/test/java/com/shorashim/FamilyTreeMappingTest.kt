package com.shorashim

import com.shorashim.core.model.FamilySummary
import com.shorashim.core.model.ScreenContentState
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyTreeMappingTest {
    @Test
    fun missingFamilyIdMapsToEmpty() {
        val summary = FamilySummary(familyId = null, name = null, personCount = 0)
        val content = if (summary.familyId == null) {
            ScreenContentState.Empty
        } else {
            ScreenContentState.Ready
        }
        assertTrue(content is ScreenContentState.Empty)
    }

    @Test
    fun existingFamilyIdMapsToReady() {
        val summary = FamilySummary(familyId = "family-1", name = "כהן", personCount = 3)
        val content = if (summary.familyId == null) {
            ScreenContentState.Empty
        } else {
            ScreenContentState.Ready
        }
        assertTrue(content is ScreenContentState.Ready)
    }
}
