package com.shorashim.core.model

import java.time.LocalDate

/**
 * Pure-Kotlin Hebrew calendar (no ICU / no Android dependency, so it runs in plain JVM unit tests).
 *
 * Months are numbered in "Tishrei-first" order, which keeps them stable across leap years:
 * 0 Tishrei, 1 Cheshvan, 2 Kislev, 3 Tevet, 4 Shevat, 5 Adar I (leap years only), 6 Adar (Adar II in leap
 * years), 7 Nisan, 8 Iyar, 9 Sivan, 10 Tamuz, 11 Av, 12 Elul.
 *
 * The conversion was validated against Hebcal for 1,000s of dates (2018-2100).
 */
data class HebrewDate(val year: Int, val month: Int, val day: Int) {
    val isLeapYear: Boolean get() = HebrewCalendarMath.isLeapYear(year)
}

object HebrewCalendarMath {
    const val TISHREI = 0
    const val CHESHVAN = 1
    const val KISLEV = 2
    const val TEVET = 3
    const val SHEVAT = 4
    const val ADAR_I = 5
    const val ADAR = 6
    const val NISAN = 7
    const val IYAR = 8
    const val SIVAN = 9
    const val TAMUZ = 10
    const val AV = 11
    const val ELUL = 12

    // 1 Tishrei 5786 == 23 September 2025 anchors the arithmetic day count to the Gregorian calendar.
    private const val ANCHOR_YEAR = 5786
    private val EPOCH_OFFSET: Long by lazy {
        LocalDate.of(2025, 9, 23).toEpochDay() - newYearDays(ANCHOR_YEAR)
    }

    fun isLeapYear(year: Int): Boolean = Math.floorMod(7 * year + 1, 19) < 7

    private fun elapsedDays(year: Int): Long {
        val months = (235L * year - 234L) / 19L
        val parts = 12084L + 13753L * months
        var day = months * 29L + parts / 25920L
        if ((3L * (day + 1L)) % 7L < 3L) day++
        return day
    }

    private fun newYearDays(year: Int): Long {
        val ed = elapsedDays(year)
        val next = elapsedDays(year + 1)
        val prev = elapsedDays(year - 1)
        val delay = when {
            next - ed == 356L -> 2
            ed - prev == 382L -> 1
            else -> 0
        }
        return ed + delay
    }

    fun yearLength(year: Int): Int = (newYearDays(year + 1) - newYearDays(year)).toInt()

    /** First day (1 Tishrei) of [year]. */
    fun roshHashana(year: Int): LocalDate = LocalDate.ofEpochDay(EPOCH_OFFSET + newYearDays(year))

    /** Month numbers that exist in [year], in calendar order. */
    fun monthsOf(year: Int): List<Int> =
        if (isLeapYear(year)) (0..12).toList() else listOf(0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12)

    fun monthLength(year: Int, month: Int): Int {
        val len = yearLength(year)
        return when (month) {
            TISHREI -> 30
            CHESHVAN -> if (len % 10 == 5) 30 else 29
            KISLEV -> if (len % 10 == 3) 29 else 30
            TEVET -> 29
            SHEVAT -> 30
            ADAR_I -> 30
            ADAR -> 29
            NISAN -> 30
            IYAR -> 29
            SIVAN -> 30
            TAMUZ -> 29
            AV -> 30
            else -> 29 // Elul
        }
    }

    fun fromLocalDate(date: LocalDate): HebrewDate {
        var year = date.year + 3760
        if (date.isBefore(roshHashana(year))) year-- else if (!date.isBefore(roshHashana(year + 1))) year++
        var dayOfYear = (date.toEpochDay() - roshHashana(year).toEpochDay()).toInt()
        for (month in monthsOf(year)) {
            val len = monthLength(year, month)
            if (dayOfYear < len) return HebrewDate(year, month, dayOfYear + 1)
            dayOfYear -= len
        }
        error("unreachable: day-of-year out of range for $date")
    }

    fun toLocalDate(year: Int, month: Int, day: Int): LocalDate {
        var offset = 0
        for (m in monthsOf(year)) {
            if (m == month) break
            offset += monthLength(year, m)
        }
        return roshHashana(year).plusDays((offset + day - 1).toLong())
    }

    fun toLocalDate(date: HebrewDate): LocalDate = toLocalDate(date.year, date.month, date.day)

    /** Month that follows [month] in [year] (Elul rolls into Tishrei of the next year). */
    fun nextMonth(year: Int, month: Int): Pair<Int, Int> {
        val months = monthsOf(year)
        val i = months.indexOf(month)
        return if (i + 1 < months.size) year to months[i + 1] else (year + 1) to TISHREI
    }

    fun monthName(year: Int, month: Int): String = when (month) {
        TISHREI -> "תשרי"
        CHESHVAN -> "חשוון"
        KISLEV -> "כסלו"
        TEVET -> "טבת"
        SHEVAT -> "שבט"
        ADAR_I -> "אדר א׳"
        ADAR -> if (isLeapYear(year)) "אדר ב׳" else "אדר"
        NISAN -> "ניסן"
        IYAR -> "אייר"
        SIVAN -> "סיוון"
        TAMUZ -> "תמוז"
        AV -> "אב"
        else -> "אלול"
    }

    /**
     * Calculates the exact Gregorian date for a Hebrew anniversary (e.g. Yahrzeit / memorial)
     * in [targetHebrewYear], handling Hebrew leap years (Adar I / Adar II) and variable month lengths.
     */
    fun getHebrewAnniversaryDate(originalDate: HebrewDate, targetHebrewYear: Int): LocalDate {
        val isTargetLeap = isLeapYear(targetHebrewYear)
        val isOriginalLeap = isLeapYear(originalDate.year)
        val targetMonth = when {
            // Died in Adar I of a leap year, but target year is not leap: observed in Adar (6)
            originalDate.month == ADAR_I && !isTargetLeap -> ADAR
            // Died in Adar of a regular year, and target year is leap: observed in Adar II (6)
            originalDate.month == ADAR && isTargetLeap && !isOriginalLeap -> ADAR
            else -> originalDate.month
        }
        val maxDays = monthLength(targetHebrewYear, targetMonth)
        val targetDay = minOf(originalDate.day, maxDays)
        return toLocalDate(targetHebrewYear, targetMonth, targetDay)
    }

    fun getHebrewAnniversaryDate(originalDate: LocalDate, targetHebrewYear: Int): LocalDate {
        return getHebrewAnniversaryDate(fromLocalDate(originalDate), targetHebrewYear)
    }
}
