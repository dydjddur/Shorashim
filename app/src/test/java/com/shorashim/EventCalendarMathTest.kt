package com.shorashim

import com.shorashim.core.common.HebrewDates
import com.shorashim.core.model.CalendarEvent
import com.shorashim.core.model.EventCalendarMath
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventCalendarMathTest {

    private fun birthday(target: String, original: String = "1990-${target.substring(5)}") = CalendarEvent.Birthday(
        id = "b", personId = "p", personName = "Dana", familyNames = emptyList(),
        birthDateHistorical = original, targetDate = LocalDate.parse(target),
        daysUntil = 0, relativeLabel = "", message = "",
    )

    @Test
    fun `september 2025 grid starts on Sunday 31 Aug and has five weeks`() {
        val grid = EventCalendarMath.monthGrid(YearMonth.of(2025, 9))
        assertEquals(35, grid.size)
        assertEquals(LocalDate.of(2025, 8, 31), grid.first())
        assertEquals(LocalDate.of(2025, 10, 4), grid.last())
    }

    @Test
    fun `month that needs six weeks gets six rows`() {
        // August 2025 starts on a Friday and has 31 days -> 6 rows.
        val grid = EventCalendarMath.monthGrid(YearMonth.of(2025, 8))
        assertEquals(42, grid.size)
    }

    @Test
    fun `grid always starts on Sunday and ends on Saturday`() {
        for (m in 1..12) {
            val grid = EventCalendarMath.monthGrid(YearMonth.of(2026, m))
            assertEquals(0, EventCalendarMath.sundayIndex(grid.first().dayOfWeek))
            assertTrue(EventCalendarMath.isSaturday(grid.last()))
        }
    }

    @Test
    fun `annual events match in any year`() {
        val e = birthday("2026-10-13")
        assertTrue(EventCalendarMath.occursOn(e, LocalDate.of(2025, 10, 13)))
        assertTrue(EventCalendarMath.occursOn(e, LocalDate.of(2031, 10, 13)))
        assertFalse(EventCalendarMath.occursOn(e, LocalDate.of(2025, 10, 14)))
    }

    @Test
    fun `leap day events fall on Feb 28 in common years and Feb 29 in leap years`() {
        val e = birthday("2027-02-28", original = "2000-02-29")
        assertTrue(EventCalendarMath.occursOn(e, LocalDate.of(2027, 2, 28)))
        assertFalse(EventCalendarMath.occursOn(e, LocalDate.of(2028, 2, 28)))
        assertTrue(EventCalendarMath.occursOn(e, LocalDate.of(2028, 2, 29)))
    }

    @Test
    fun `hebrew numerals`() {
        assertEquals("א", HebrewDates.toGematria(1))
        assertEquals("טו", HebrewDates.toGematria(15))
        assertEquals("טז", HebrewDates.toGematria(16))
        assertEquals("כח", HebrewDates.toGematria(28))
        assertEquals("ל", HebrewDates.toGematria(30))
        assertEquals("תשפו", HebrewDates.toGematria(786))
        assertEquals("תשעה", HebrewDates.toGematria(775))
    }
}
