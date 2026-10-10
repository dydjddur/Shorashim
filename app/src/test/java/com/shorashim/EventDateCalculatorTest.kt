package com.shorashim

import com.shorashim.core.model.EventDateCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EventDateCalculatorTest {

    @Test
    fun calculateNextBirthday_laterThisYear() {
        val today = LocalDate.of(2026, 5, 10)
        val next = EventDateCalculator.calculateNextBirthday("1995-08-20", today)
        assertEquals(LocalDate.of(2026, 8, 20), next)
    }

    @Test
    fun calculateNextBirthday_alreadyPassedThisYear_returnsNextYear() {
        val today = LocalDate.of(2026, 5, 10)
        val next = EventDateCalculator.calculateNextBirthday("1995-03-15", today)
        assertEquals(LocalDate.of(2027, 3, 15), next)
    }

    @Test
    fun calculateNextBirthday_today_returnsToday() {
        val today = LocalDate.of(2026, 5, 10)
        val next = EventDateCalculator.calculateNextBirthday("2000-05-10", today)
        assertEquals(today, next)
    }

    @Test
    fun calculateNextBirthday_feb29_nonLeapYear_fallsBackToFeb28() {
        val today = LocalDate.of(2026, 1, 1) // 2026 is non-leap
        val next = EventDateCalculator.calculateNextBirthday("2000-02-29", today)
        assertEquals(LocalDate.of(2026, 2, 28), next)
    }

    @Test
    fun calculateNextBirthday_feb29_leapYear_keepsFeb29() {
        val today = LocalDate.of(2028, 1, 1) // 2028 is leap year
        val next = EventDateCalculator.calculateNextBirthday("2000-02-29", today)
        assertEquals(LocalDate.of(2028, 2, 29), next)
    }

    @Test
    fun calculateNextBirthday_invalidOrBlank_returnsNull() {
        val today = LocalDate.of(2026, 5, 10)
        assertNull(EventDateCalculator.calculateNextBirthday(null, today))
        assertNull(EventDateCalculator.calculateNextBirthday("", today))
        assertNull(EventDateCalculator.calculateNextBirthday("not-a-date", today))
    }

    @Test
    fun formatRelativeLabel_formatsCorrectly() {
        val today = LocalDate.of(2026, 5, 10)
        assertEquals("היום", EventDateCalculator.formatRelativeLabel(today, today))
        assertEquals("מחר", EventDateCalculator.formatRelativeLabel(today.plusDays(1), today))
        assertEquals("בעוד 3 ימים", EventDateCalculator.formatRelativeLabel(today.plusDays(3), today))
        assertEquals("בעוד שבוע", EventDateCalculator.formatRelativeLabel(today.plusDays(7), today))
        assertEquals("בעוד שבועיים", EventDateCalculator.formatRelativeLabel(today.plusDays(14), today))
    }

    @Test
    fun getBirthdayMessage_correctHebrewWording() {
        val todayMsg = EventDateCalculator.getBirthdayMessage("יוסי כהן", isToday = true)
        assertTrue(todayMsg.contains("ליוסי כהן יש היום יום הולדת!"))
        assertTrue(todayMsg.contains("🎉"))

        val futureMsg = EventDateCalculator.getBirthdayMessage("יוסי כהן", isToday = false)
        assertTrue(futureMsg.contains("יום ההולדת של יוסי כהן מתקרב!"))
        assertTrue(futureMsg.contains("🎂"))
    }
}
