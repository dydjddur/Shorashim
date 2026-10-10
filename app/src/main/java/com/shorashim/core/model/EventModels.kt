package com.shorashim.core.model

import java.time.LocalDate
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Unified representation of events shown in the "אירועים" (Events) tab.
 */
sealed interface CalendarEvent {
    val id: String
    val personName: String
    val targetDate: LocalDate
    val daysUntil: Long
    val relativeLabel: String

    data class Birthday(
        override val id: String,
        val personId: String,
        override val personName: String,
        val familyNames: List<String>,
        val birthDateHistorical: String,
        override val targetDate: LocalDate,
        override val daysUntil: Long,
        override val relativeLabel: String,
        val message: String,
    ) : CalendarEvent

    data class Memorial(
        override val id: String,
        val memorialId: String,
        val personId: String?,
        override val personName: String,
        val familyId: String,
        val familyName: String,
        val originalDateStr: String,
        override val targetDate: LocalDate,
        val hebrewDate: String?,
        val description: String?,
        override val daysUntil: Long,
        override val relativeLabel: String,
        val canEdit: Boolean = false,
    ) : CalendarEvent
}

/**
 * Utility for Israel timezone calculations, leap-year handling (Feb 29 -> Feb 28),
 * and Hebrew relative time labels.
 */
object EventDateCalculator {
    val ISRAEL_ZONE: ZoneId = ZoneId.of("Asia/Jerusalem")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun getTodayInIsrael(): LocalDate = LocalDate.now(ISRAEL_ZONE)

    /**
     * Calculates the next upcoming birthday for a person based on their birth date (YYYY-MM-DD).
     * If the birthday has already passed this year, returns the occurrence next year.
     * Handles Feb 29 on non-leap years by falling back to Feb 28.
     * Returns null if birth date is null or invalid.
     */
    fun calculateNextBirthday(
        birthDateStr: String?,
        today: LocalDate = getTodayInIsrael(),
    ): LocalDate? {
        if (birthDateStr.isNullOrBlank()) return null
        val birthDate = try {
            LocalDate.parse(birthDateStr.trim())
        } catch (_: Throwable) {
            return null
        }

        val month = birthDate.monthValue
        val day = birthDate.dayOfMonth

        val currentYear = today.year
        val thisYearCandidate = makeOccurrence(currentYear, month, day)

        return if (!thisYearCandidate.isBefore(today)) {
            thisYearCandidate
        } else {
            makeOccurrence(currentYear + 1, month, day)
        }
    }

    /**
     * Calculates the next upcoming memorial occurrence for a memorial date.
     * If the date has already passed in the current year, returns next year's annual anniversary.
     */
    fun calculateNextMemorialDate(
        eventDateStr: String,
        today: LocalDate = getTodayInIsrael(),
    ): LocalDate? {
        if (eventDateStr.isBlank()) return null
        val originalDate = try {
            LocalDate.parse(eventDateStr.trim())
        } catch (_: Throwable) {
            return null
        }

        val month = originalDate.monthValue
        val day = originalDate.dayOfMonth

        val currentYear = today.year
        val thisYearCandidate = makeOccurrence(currentYear, month, day)

        return if (!thisYearCandidate.isBefore(today)) {
            thisYearCandidate
        } else {
            makeOccurrence(currentYear + 1, month, day)
        }
    }

    private fun makeOccurrence(year: Int, month: Int, day: Int): LocalDate {
        val isLeap = Year.isLeap(year.toLong())
        return if (month == 2 && day == 29 && !isLeap) {
            LocalDate.of(year, 2, 28)
        } else {
            LocalDate.of(year, month, day)
        }
    }

    fun calculateDaysUntil(targetDate: LocalDate, today: LocalDate = getTodayInIsrael()): Long {
        return ChronoUnit.DAYS.between(today, targetDate)
    }

    fun formatRelativeLabel(targetDate: LocalDate, today: LocalDate = getTodayInIsrael()): String {
        val days = calculateDaysUntil(targetDate, today)
        return when {
            days == 0L -> "היום"
            days == 1L -> "מחר"
            days in 2L..6L -> "בעוד $days ימים"
            days in 7L..13L -> "בעוד שבוע"
            days in 14L..20L -> "בעוד שבועיים"
            else -> targetDate.format(DATE_FORMATTER)
        }
    }

    fun getBirthdayMessage(personName: String, isToday: Boolean): String {
        return if (isToday) {
            "ל$personName יש היום יום הולדת! שלחו לו מזל טוב, תשמחו אותו ותעשו לו יום שמח! 🎉"
        } else {
            "יום ההולדת של $personName מתקרב! אל תשכחו לאחל לו מזל טוב 🎂"
        }
    }
}
