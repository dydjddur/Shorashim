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

    /** Formats a day number (1..30) as Hebrew letters with gershayim/geresh, e.g. 1 -> "א׳", 15 -> "ט״ו". */
    fun formatHebrewDay(day: Int): String {
        if (day <= 0) return ""
        val letters = toGematria(day)
        return when {
            letters.length > 1 -> letters.dropLast(1) + "״" + letters.last()
            letters.length == 1 -> "$letters׳"
            else -> day.toString()
        }
    }

    /** Formats a Hebrew year (e.g. 5786 -> "ה׳תשפ״ו" or "תשפ״ו"). */
    fun formatHebrewYear(year: Int, includeThousands: Boolean = true): String {
        if (year <= 0) return ""
        val letters = toGematria(year % 1000)
        val withGershayim = when {
            letters.length > 1 -> letters.dropLast(1) + "״" + letters.last()
            letters.length == 1 -> "$letters׳"
            else -> year.toString()
        }
        return if (includeThousands) "ה׳$withGershayim" else withGershayim
    }

    /** Formats a full Hebrew date, e.g. `HebrewDate(5784, SHEVAT, 15)` -> "ט״ו בשבט ה׳תשפ״ד". */
    fun formatHebrewDate(hebrewDate: com.shorashim.core.model.HebrewDate, includeThousands: Boolean = true): String {
        val dayStr = formatHebrewDay(hebrewDate.day)
        val monthStr = HebrewCalendarMath.monthName(hebrewDate.year, hebrewDate.month)
        val yearStr = formatHebrewYear(hebrewDate.year, includeThousands)
        return "$dayStr ב$monthStr $yearStr"
    }

    /** Formats a Gregorian LocalDate as a full Hebrew date string. */
    fun formatHebrewDate(date: LocalDate, includeThousands: Boolean = true): String {
        return formatHebrewDate(HebrewCalendarMath.fromLocalDate(date), includeThousands)
    }

    /**
     * Parses a Hebrew numeral string or decimal string into a number.
     * Supports e.g. "15", "טו", "ט״ו", "5784", "תשפד", "ה׳תשפ״ד".
     */
    fun parseGematria(text: String): Int? {
        val clean = text.replace("״", "").replace("'", "").replace("׳", "").replace("\"", "").replace(" ", "").trim()
        val num = clean.toIntOrNull()
        if (num != null) return num
        var total = 0
        var hasLetters = false
        // Skip leading 'ה' if followed by thousands
        val lettersOnly = if (clean.startsWith("ה") && clean.length > 3) clean.substring(1) else clean
        for (ch in lettersOnly) {
            val v = when (ch) {
                'א' -> 1; 'ב' -> 2; 'ג' -> 3; 'ד' -> 4; 'ה' -> 5; 'ו' -> 6; 'ז' -> 7; 'ח' -> 8; 'ט' -> 9
                'י' -> 10; 'כ', 'ך' -> 20; 'ל' -> 30; 'מ', 'ם' -> 40; 'נ', 'ן' -> 50
                'ס' -> 60; 'ע' -> 70; 'פ', 'ף' -> 80; 'צ', 'ץ' -> 90
                'ק' -> 100; 'ר' -> 200; 'ש' -> 300; 'ת' -> 400
                else -> null
            }
            if (v != null) {
                total += v
                hasLetters = true
            }
        }
        if (!hasLetters) return null
        if (total in 500..999) {
            // Assume 5000s if likely a Hebrew year (e.g. תשפ"ד = 784 -> 5784)
            return total + 5000
        }
        return total
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
