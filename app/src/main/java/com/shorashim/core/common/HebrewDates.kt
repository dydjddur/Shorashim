package com.shorashim.core.common

import android.icu.text.SimpleDateFormat
import android.icu.util.HebrewCalendar
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * Hebrew-calendar helpers used by the events calendar.
 *
 * [toGematria] is pure Kotlin (unit-testable); everything else is backed by ICU's [HebrewCalendar],
 * which ships with Android (API 24+).
 */
object HebrewDates {

    private val HEBREW_LOCALE: Locale = Locale.forLanguageTag("he-IL")

    private val UNITS = arrayOf("", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט")
    private val TENS = arrayOf("", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ")
    private val HUNDREDS = arrayOf("", "ק", "ר", "ש", "ת")

    /** Converts a positive number to Hebrew numerals (15 -> "טו", 16 -> "טז", 786 -> "תשפו"). */
    fun toGematria(number: Int): String {
        if (number <= 0) return ""
        var n = number
        val sb = StringBuilder()
        while (n >= 400) {
            sb.append("ת")
            n -= 400
        }
        if (n >= 100) {
            sb.append(HUNDREDS[n / 100])
            n %= 100
        }
        when (n) {
            15 -> sb.append("טו")
            16 -> sb.append("טז")
            else -> {
                sb.append(TENS[n / 10])
                sb.append(UNITS[n % 10])
            }
        }
        return sb.toString()
    }

    /** Hebrew day of month as letters, e.g. 28 -> "כח". */
    fun dayLabel(date: LocalDate): String = toGematria(calendarFor(date).get(HebrewCalendar.DATE))

    /** True when [date] is the first day of a Hebrew month (Rosh Chodesh day 1). */
    fun isFirstOfHebrewMonth(date: LocalDate): Boolean = calendarFor(date).get(HebrewCalendar.DATE) == 1

    /** Hebrew month name, e.g. "תשרי". */
    fun monthName(date: LocalDate): String = formatter("MMMM", date).trim()

    /** Hebrew year with the thousands dropped and gershayim, e.g. 5786 -> `תשפ"ו`. */
    fun yearLabel(date: LocalDate): String {
        val year = calendarFor(date).get(HebrewCalendar.YEAR)
        val letters = toGematria(year % 1000)
        return when {
            letters.length > 1 -> letters.dropLast(1) + "\"" + letters.last()
            letters.length == 1 -> "$letters'"
            else -> year.toString()
        }
    }

    /** "אלול-תשרי ה'תשפ"ו" style range for the Gregorian month starting at [first] and ending at [last]. */
    fun rangeLabel(first: LocalDate, last: LocalDate): String {
        val startMonth = monthName(first)
        val endMonth = monthName(last)
        val startYear = yearLabel(first)
        val endYear = yearLabel(last)
        val months = if (startMonth == endMonth) startMonth else "$startMonth-$endMonth"
        val years = if (startYear == endYear) "ה'$startYear" else "ה'$startYear-ה'$endYear"
        return "$months $years"
    }

    private fun calendarFor(date: LocalDate): HebrewCalendar {
        val cal = HebrewCalendar(HEBREW_LOCALE)
        cal.timeInMillis = date.atTime(12, 0).atZone(ZoneId.of("Asia/Jerusalem")).toInstant().toEpochMilli()
        return cal
    }

    private fun formatter(pattern: String, date: LocalDate): String {
        val cal = calendarFor(date)
        val fmt = SimpleDateFormat(pattern, HEBREW_LOCALE)
        fmt.calendar = cal
        return fmt.format(Date(cal.timeInMillis))
    }
}
