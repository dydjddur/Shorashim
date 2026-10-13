package com.shorashim.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure date helpers for the month-grid view of the events tab.
 *
 * Birthdays and memorials are *annual*: [CalendarEvent.targetDate] is only the next occurrence, so the
 * calendar matches events by month/day and shows them in every year.
 */
object EventCalendarMath {

    /** Israeli weeks start on Sunday. */
    private const val DAYS_IN_WEEK = 7

    /** Dates shown for [month]: whole weeks, Sunday first, including leading/trailing neighbour days. */
    fun monthGrid(month: YearMonth): List<LocalDate> {
        val first = month.atDay(1)
        val leading = sundayIndex(first.dayOfWeek)
        val total = leading + month.lengthOfMonth()
        val weeks = (total + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK
        val start = first.minusDays(leading.toLong())
        return List(weeks * DAYS_IN_WEEK) { start.plusDays(it.toLong()) }
    }

    /** 0 = Sunday ... 6 = Saturday. */
    fun sundayIndex(day: DayOfWeek): Int = day.value % DAYS_IN_WEEK

    fun isSaturday(date: LocalDate): Boolean = date.dayOfWeek == DayOfWeek.SATURDAY

    /** Whether the annual [event] falls on [date] (Feb 29 events fall on Feb 28 in non-leap years). */
    fun occursOn(event: CalendarEvent, date: LocalDate): Boolean {
        return when (event) {
            is CalendarEvent.Birthday -> {
                if (event.originalIsLeapDay()) {
                    val expectedDay = if (date.isLeapYear) 29 else 28
                    return date.monthValue == 2 && date.dayOfMonth == expectedDay
                }
                val target = event.targetDate
                target.month == date.month && target.dayOfMonth == date.dayOfMonth
            }
            is CalendarEvent.Memorial -> {
                occursOnMemorial(event, date)
            }
        }
    }

    private fun occursOnMemorial(event: CalendarEvent.Memorial, date: LocalDate): Boolean {
        if (event.targetDate == date) return true

        val origDate = parseOriginalMemorialDate(event.originalDateStr)
        if (origDate != null) {
            val hOriginal = HebrewCalendarMath.fromLocalDate(origDate)
            val hDate = HebrewCalendarMath.fromLocalDate(date)
            val isLeapTarget = HebrewCalendarMath.isLeapYear(hDate.year)
            val isLeapOrig = HebrewCalendarMath.isLeapYear(hOriginal.year)
            val targetMonth = when {
                hOriginal.month == HebrewCalendarMath.ADAR_I && !isLeapTarget -> HebrewCalendarMath.ADAR
                hOriginal.month == HebrewCalendarMath.ADAR && isLeapTarget && !isLeapOrig -> HebrewCalendarMath.ADAR
                else -> hOriginal.month
            }
            val maxDays = HebrewCalendarMath.monthLength(hDate.year, targetMonth)
            val targetDay = minOf(hOriginal.day, maxDays)
            if (hDate.month == targetMonth && hDate.day == targetDay) {
                return true
            }
        }

        val target = event.targetDate
        return target.month == date.month && target.dayOfMonth == date.dayOfMonth
    }

    private fun parseOriginalMemorialDate(str: String): LocalDate? {
        if (str.isBlank()) return null
        return try {
            if (str.contains("/")) {
                val parts = str.split("/")
                if (parts.size == 3) {
                    val d = parts[0].toInt()
                    val m = parts[1].toInt()
                    val y = parts[2].toInt()
                    LocalDate.of(y, m, d)
                } else null
            } else {
                LocalDate.parse(str.trim())
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun eventsOn(events: List<CalendarEvent>, date: LocalDate): List<CalendarEvent> =
        events.filter { occursOn(it, date) }.sortedWith(compareBy({ it is CalendarEvent.Memorial }, { it.personName }))

    private fun CalendarEvent.originalIsLeapDay(): Boolean = when (this) {
        is CalendarEvent.Birthday -> birthDateHistorical.endsWith("-02-29")
        is CalendarEvent.Memorial -> originalDateStr.startsWith("29/02/")
    }

    /** Previous / next month helpers kept here so UI code stays free of arithmetic. */
    fun previous(month: YearMonth): YearMonth = month.minusMonths(1)

    fun next(month: YearMonth): YearMonth = month.plusMonths(1)
}
