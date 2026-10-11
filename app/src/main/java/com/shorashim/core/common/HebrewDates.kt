package com.shorashim.core.common

import com.shorashim.core.model.HebrewCalendarMath
import java.time.LocalDate

/**
 * Hebrew-calendar display helpers used by the events calendar (pure Kotlin, unit-testable).
 */
object HebrewDates {

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
    fun dayLabel(date: LocalDate): String = toGematria(HebrewCalendarMath.fromLocalDate(date).day)

    /** True when [date] is the first day of a Hebrew month. */
    fun isFirstOfHebrewMonth(date: LocalDate): Boolean = HebrewCalendarMath.fromLocalDate(date).day == 1

    /** Hebrew month name, e.g. "תשרי". */
    fun monthName(date: LocalDate): String {
        val h = HebrewCalendarMath.fromLocalDate(date)
        return HebrewCalendarMath.monthName(h.year, h.month)
    }

    /** Hebrew year with the thousands dropped and gershayim, e.g. 5786 -> `תשפ"ו`. */
    fun yearLabel(date: LocalDate): String {
        val year = HebrewCalendarMath.fromLocalDate(date).year
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
}
