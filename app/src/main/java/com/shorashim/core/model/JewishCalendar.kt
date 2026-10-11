package com.shorashim.core.model

import com.shorashim.core.model.HebrewCalendarMath.ADAR
import com.shorashim.core.model.HebrewCalendarMath.ADAR_I
import com.shorashim.core.model.HebrewCalendarMath.AV
import com.shorashim.core.model.HebrewCalendarMath.CHESHVAN
import com.shorashim.core.model.HebrewCalendarMath.ELUL
import com.shorashim.core.model.HebrewCalendarMath.IYAR
import com.shorashim.core.model.HebrewCalendarMath.KISLEV
import com.shorashim.core.model.HebrewCalendarMath.NISAN
import com.shorashim.core.model.HebrewCalendarMath.SHEVAT
import com.shorashim.core.model.HebrewCalendarMath.SIVAN
import com.shorashim.core.model.HebrewCalendarMath.TAMUZ
import com.shorashim.core.model.HebrewCalendarMath.TEVET
import com.shorashim.core.model.HebrewCalendarMath.TISHREI
import java.time.DayOfWeek
import java.time.LocalDate

enum class JewishDayKind {
    /** Biblical festival days (work-free): Rosh Hashana, Yom Kippur, Sukkot, Pesach, Shavuot... */
    YOM_TOV,
    CHOL_HAMOED,
    FAST,
    EREV,
    ROSH_CHODESH,
    CHANUKAH,
    PURIM,
    MINOR,
    MODERN,
    SPECIAL_SHABBAT,
    PARASHA,
}

data class JewishDay(val id: String, val title: String, val kind: JewishDayKind) {
    val isYomTov: Boolean get() = kind == JewishDayKind.YOM_TOV
}

/**
 * Jewish holidays, fasts, special Shabbatot and the weekly Torah portion for any Gregorian date, using the
 * **Israeli** schedule (single-day festivals outside Pesach/Sukkot, combined Shmini Atzeret / Simchat Torah).
 *
 * Verified against Hebcal's Israel calendar for 2018-2100: every holiday, fast, Rosh Chodesh, special
 * Shabbat and every weekly portion (including double portions) matches.
 */
object JewishCalendar {

    internal val PARASHOT = listOf(
        "בראשית", "נח", "לך לך", "וירא", "חיי שרה", "תולדות", "ויצא", "וישלח", "וישב", "מקץ", "ויגש", "ויחי",
        "שמות", "וארא", "בא", "בשלח", "יתרו", "משפטים", "תרומה", "תצוה", "כי תשא", "ויקהל", "פקודי",
        "ויקרא", "צו", "שמיני", "תזריע", "מצורע", "אחרי מות", "קדושים", "אמור", "בהר", "בחוקותי",
        "במדבר", "נשא", "בהעלותך", "שלח", "קרח", "חקת", "בלק", "פנחס", "מטות", "מסעי",
        "דברים", "ואתחנן", "עקב", "ראה", "שופטים", "כי תצא", "כי תבוא", "נצבים", "וילך", "האזינו",
    )
    private const val BESHALACH = 15

    // First index of each portion pair that may be read together.
    private const val VAYAKHEL = 21
    private const val TAZRIA = 26
    private const val ACHREI = 28
    private const val BEHAR = 31
    private const val MATOT = 41
    private const val NITZAVIM = 50

    private val GEMATRIA_DAYS = listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ז׳", "ח׳")

    /** All events of [date], most important first. */
    fun daysFor(date: LocalDate): List<JewishDay> {
        val h = HebrewCalendarMath.fromLocalDate(date)
        val dow = date.dayOfWeek
        val out = mutableListOf<JewishDay>()
        fun add(id: String, title: String, kind: JewishDayKind) {
            out.add(JewishDay(id, title, kind))
        }

        when (h.month) {
            TISHREI -> {
                when (h.day) {
                    1 -> add("rosh_hashana_1", "ראש השנה", JewishDayKind.YOM_TOV)
                    2 -> add("rosh_hashana_2", "ראש השנה ב׳", JewishDayKind.YOM_TOV)
                    3 -> if (dow != DayOfWeek.SATURDAY) add("tzom_gedaliah", "צום גדליה", JewishDayKind.FAST)
                    4 -> if (dow == DayOfWeek.SUNDAY) add("tzom_gedaliah", "צום גדליה", JewishDayKind.FAST)
                    9 -> add("erev_yom_kippur", "ערב יום כיפור", JewishDayKind.EREV)
                    10 -> add("yom_kippur", "יום כיפור", JewishDayKind.YOM_TOV)
                    14 -> add("erev_sukkot", "ערב סוכות", JewishDayKind.EREV)
                    15 -> add("sukkot_1", "סוכות", JewishDayKind.YOM_TOV)
                    in 16..20 -> add("sukkot_chm_${h.day}", "חול המועד סוכות", JewishDayKind.CHOL_HAMOED)
                    21 -> add("hoshana_raba", "הושענא רבה", JewishDayKind.CHOL_HAMOED)
                    22 -> add("shmini_atzeret", "שמיני עצרת – שמחת תורה", JewishDayKind.YOM_TOV)
                }
            }
            CHESHVAN -> {
                // 29 Cheshvan; when that falls on Shabbat it is observed on Thursday (27 Cheshvan).
                val twentyNinth = HebrewCalendarMath.toLocalDate(h.year, CHESHVAN, 29)
                val observed = if (twentyNinth.dayOfWeek == DayOfWeek.SATURDAY) twentyNinth.minusDays(2) else twentyNinth
                if (date == observed) add("sigd", "סיגד", JewishDayKind.MODERN)
            }
            TEVET -> if (h.day == 10) add("asara_btevet", "עשרה בטבת", JewishDayKind.FAST)
            SHEVAT -> if (h.day == 15) add("tu_bishvat", "ט״ו בשבט", JewishDayKind.MINOR)
            ADAR_I -> if (h.isLeapYear) {
                if (h.day == 14) add("purim_katan", "פורים קטן", JewishDayKind.MINOR)
                if (h.day == 15) add("shushan_purim_katan", "שושן פורים קטן", JewishDayKind.MINOR)
            }
            ADAR -> {
                if (h.day == 13 && dow != DayOfWeek.SATURDAY) add("taanit_esther", "תענית אסתר", JewishDayKind.FAST)
                if (h.day == 11 && dow == DayOfWeek.THURSDAY) add("taanit_esther", "תענית אסתר", JewishDayKind.FAST)
                if (h.day == 13) add("erev_purim", "ערב פורים", JewishDayKind.EREV)
                if (h.day == 14) add("purim", "פורים", JewishDayKind.PURIM)
                if (h.day == 15) add("shushan_purim", "שושן פורים", JewishDayKind.PURIM)
            }
            NISAN -> {
                when (h.day) {
                    12 -> if (dow == DayOfWeek.THURSDAY) add("taanit_bechorot", "תענית בכורות", JewishDayKind.FAST)
                    14 -> {
                        if (dow != DayOfWeek.SATURDAY) add("taanit_bechorot", "תענית בכורות", JewishDayKind.FAST)
                        add("erev_pesach", "ערב פסח", JewishDayKind.EREV)
                    }
                    15 -> add("pesach_1", "פסח", JewishDayKind.YOM_TOV)
                    in 16..20 -> add("pesach_chm_${h.day}", "חול המועד פסח", JewishDayKind.CHOL_HAMOED)
                    21 -> add("pesach_7", "שביעי של פסח", JewishDayKind.YOM_TOV)
                }
                if (date == shoahDay(h.year)) add("yom_hashoah", "יום השואה", JewishDayKind.MODERN)
            }
            IYAR -> {
                val independence = independenceDay(h.year)
                if (date == independence.minusDays(1)) add("yom_hazikaron", "יום הזיכרון", JewishDayKind.MODERN)
                if (date == independence) add("yom_haatzmaut", "יום העצמאות", JewishDayKind.MODERN)
                if (h.day == 14) add("pesach_sheni", "פסח שני", JewishDayKind.MINOR)
                if (h.day == 18) add("lag_baomer", "ל״ג בעומר", JewishDayKind.MINOR)
                if (h.day == 28) add("yom_yerushalayim", "יום ירושלים", JewishDayKind.MODERN)
            }
            SIVAN -> {
                if (h.day == 5) add("erev_shavuot", "ערב שבועות", JewishDayKind.EREV)
                if (h.day == 6) add("shavuot", "שבועות", JewishDayKind.YOM_TOV)
            }
            TAMUZ -> {
                if (h.day == 17 && dow != DayOfWeek.SATURDAY) add("tzom_tammuz", "צום י״ז בתמוז", JewishDayKind.FAST)
                if (h.day == 18 && dow == DayOfWeek.SUNDAY) add("tzom_tammuz", "צום י״ז בתמוז", JewishDayKind.FAST)
            }
            AV -> {
                val ninth = HebrewCalendarMath.toLocalDate(h.year, AV, 9)
                val ninthOnShabbat = ninth.dayOfWeek == DayOfWeek.SATURDAY
                if (h.day == 8 && !ninthOnShabbat) add("erev_tisha_bav", "ערב תשעה באב", JewishDayKind.EREV)
                if (h.day == 9) {
                    if (ninthOnShabbat) add("erev_tisha_bav", "ערב תשעה באב", JewishDayKind.EREV)
                    else add("tisha_bav", "תשעה באב", JewishDayKind.FAST)
                }
                if (h.day == 10 && dow == DayOfWeek.SUNDAY) add("tisha_bav", "תשעה באב (נדחה)", JewishDayKind.FAST)
                if (h.day == 15) add("tu_bav", "ט״ו באב", JewishDayKind.MINOR)
            }
            ELUL -> if (h.day == 29) add("erev_rosh_hashana", "ערב ראש השנה", JewishDayKind.EREV)
        }

        chanukahDay(date)?.let { n ->
            add("chanukah_$n", "חנוכה · יום ${GEMATRIA_DAYS[n - 1]}", JewishDayKind.CHANUKAH)
        }

        // Rosh Chodesh: the 30th of a full month and the 1st of the next (but never Tishrei).
        if (h.day == 30) {
            val (ny, nm) = HebrewCalendarMath.nextMonth(h.year, h.month)
            add("rosh_chodesh", "ראש חודש ${HebrewCalendarMath.monthName(ny, nm)}", JewishDayKind.ROSH_CHODESH)
        } else if (h.day == 1 && h.month != TISHREI) {
            add("rosh_chodesh", "ראש חודש ${HebrewCalendarMath.monthName(h.year, h.month)}", JewishDayKind.ROSH_CHODESH)
        }

        if (dow == DayOfWeek.SATURDAY) {
            specialShabbat(date, h)?.let { out.add(it) }
            parashaFor(date)?.let { out.add(JewishDay("parasha", "פרשת $it", JewishDayKind.PARASHA)) }
        }

        return out.sortedBy { priority(it.kind) }
    }

    /** Weekly portion read on [saturday] in Israel, or null when the Shabbat is a festival. */
    fun parashaFor(saturday: LocalDate): String? {
        if (saturday.dayOfWeek != DayOfWeek.SATURDAY) return null
        val h = HebrewCalendarMath.fromLocalDate(saturday)
        val simchatTorah = HebrewCalendarMath.toLocalDate(h.year, TISHREI, 22)
        val cycleYear = if (saturday.isAfter(simchatTorah)) h.year else h.year - 1
        val readings = readingsOfCycle(cycleYear)
        return readings[saturday]
    }

    private val cycleCache = HashMap<Int, Map<LocalDate, String>>()

    /** Map Shabbat date -> reading for the cycle that starts after Simchat Torah of [cycleYear]. */
    @Synchronized
    private fun readingsOfCycle(cycleYear: Int): Map<LocalDate, String> = cycleCache.getOrPut(cycleYear) {
        val start = HebrewCalendarMath.toLocalDate(cycleYear, TISHREI, 22)
        val end = HebrewCalendarMath.toLocalDate(cycleYear + 1, TISHREI, 22)
        var shabbat = start.plusDays(1)
        while (shabbat.dayOfWeek != DayOfWeek.SATURDAY) shabbat = shabbat.plusDays(1)

        val regular = mutableListOf<LocalDate>()
        while (shabbat.isBefore(end)) {
            val h = HebrewCalendarMath.fromLocalDate(shabbat)
            val festival = (h.month == NISAN && h.day in 15..21) ||
                (h.month == SIVAN && h.day == 6) ||
                (h.month == TISHREI && (h.day == 1 || h.day == 2 || h.day == 10 || h.day in 15..21))
            if (!festival) regular.add(shabbat)
            shabbat = shabbat.plusDays(7)
        }

        val combined = combinedPairs(cycleYear)
        val names = mutableListOf<String>()
        var i = 0
        while (i < PARASHOT.size) {
            if (i in combined) {
                names.add("${PARASHOT[i]}-${PARASHOT[i + 1]}")
                i += 2
            } else {
                names.add(PARASHOT[i])
                i++
            }
        }
        // Sanity: the rules guarantee names.size == regular.size; if data were ever off, map what we can.
        regular.zip(names).toMap()
    }

    /**
     * Which double portions are read in the cycle of Hebrew year [year] (Israel).
     * Derived from the year type (weekday of Rosh Hashana + year length) and validated against Hebcal.
     */
    private fun combinedPairs(year: Int): Set<Int> {
        val length = HebrewCalendarMath.yearLength(year)
        val rh = HebrewCalendarMath.roshHashana(year).dayOfWeek
        val nextRh = HebrewCalendarMath.roshHashana(year + 1).dayOfWeek
        val pairs = mutableSetOf<Int>()
        if (nextRh == DayOfWeek.THURSDAY || nextRh == DayOfWeek.SATURDAY) pairs.add(NITZAVIM)
        if (!HebrewCalendarMath.isLeapYear(year)) {
            pairs.add(TAZRIA)
            pairs.add(ACHREI)
            pairs.add(MATOT)
            if (!(length == 354 && rh == DayOfWeek.THURSDAY)) pairs.add(BEHAR)
            if (!(length == 355 && rh == DayOfWeek.THURSDAY)) pairs.add(VAYAKHEL)
        } else if ((length == 383 && rh != DayOfWeek.THURSDAY) || (length == 385 && rh == DayOfWeek.SATURDAY)) {
            pairs.add(MATOT)
        }
        return pairs
    }

    private fun specialShabbat(date: LocalDate, h: HebrewDate): JewishDay? {
        fun satOnOrBefore(d: LocalDate): LocalDate {
            var x = d
            while (x.dayOfWeek != DayOfWeek.SATURDAY) x = x.minusDays(1)
            return x
        }
        fun satBefore(d: LocalDate) = satOnOrBefore(d.minusDays(1))

        for (year in listOf(h.year, h.year + 1)) {
            if (date == satOnOrBefore(HebrewCalendarMath.toLocalDate(year, ADAR, 1))) {
                return JewishDay("shabbat_shekalim", "שבת שקלים", JewishDayKind.SPECIAL_SHABBAT)
            }
            if (date == satBefore(HebrewCalendarMath.toLocalDate(year, ADAR, 14))) {
                return JewishDay("shabbat_zachor", "שבת זכור", JewishDayKind.SPECIAL_SHABBAT)
            }
            val hachodesh = satOnOrBefore(HebrewCalendarMath.toLocalDate(year, NISAN, 1))
            if (date == hachodesh) return JewishDay("shabbat_hachodesh", "שבת החודש", JewishDayKind.SPECIAL_SHABBAT)
            if (date == satBefore(hachodesh)) return JewishDay("shabbat_parah", "שבת פרה", JewishDayKind.SPECIAL_SHABBAT)
            if (date == satBefore(HebrewCalendarMath.toLocalDate(year, NISAN, 15))) {
                return JewishDay("shabbat_hagadol", "שבת הגדול", JewishDayKind.SPECIAL_SHABBAT)
            }
            val ninthAv = HebrewCalendarMath.toLocalDate(year, AV, 9)
            if (date == satOnOrBefore(ninthAv)) return JewishDay("shabbat_chazon", "שבת חזון", JewishDayKind.SPECIAL_SHABBAT)
            if (date.isAfter(ninthAv) && date == satBefore(ninthAv.plusDays(8))) {
                return JewishDay("shabbat_nachamu", "שבת נחמו", JewishDayKind.SPECIAL_SHABBAT)
            }
        }
        if (h.month == TISHREI && h.day in 3..9) {
            return JewishDay("shabbat_shuva", "שבת שובה", JewishDayKind.SPECIAL_SHABBAT)
        }
        if (parashaFor(date) == PARASHOT[BESHALACH]) {
            return JewishDay("shabbat_shirah", "שבת שירה", JewishDayKind.SPECIAL_SHABBAT)
        }
        return null
    }

    /** 1..8 when [date] is a day of Chanukah (25 Kislev = day 1), else null. */
    private fun chanukahDay(date: LocalDate): Int? {
        val h = HebrewCalendarMath.fromLocalDate(date)
        for (y in listOf(h.year, h.year - 1)) {
            val start = HebrewCalendarMath.toLocalDate(y, KISLEV, 25)
            val n = (date.toEpochDay() - start.toEpochDay()).toInt()
            if (n in 0..7) return n + 1
        }
        return null
    }

    private fun independenceDay(year: Int): LocalDate {
        val fifth = HebrewCalendarMath.toLocalDate(year, IYAR, 5)
        return when (fifth.dayOfWeek) {
            DayOfWeek.FRIDAY -> fifth.minusDays(1)
            DayOfWeek.SATURDAY -> fifth.minusDays(2)
            DayOfWeek.MONDAY -> fifth.plusDays(1)
            else -> fifth
        }
    }

    private fun shoahDay(year: Int): LocalDate {
        val d = HebrewCalendarMath.toLocalDate(year, NISAN, 27)
        return when (d.dayOfWeek) {
            DayOfWeek.FRIDAY -> d.minusDays(1)
            DayOfWeek.SUNDAY -> d.plusDays(1)
            else -> d
        }
    }

    private fun priority(kind: JewishDayKind): Int = when (kind) {
        JewishDayKind.YOM_TOV -> 0
        JewishDayKind.CHOL_HAMOED -> 1
        JewishDayKind.FAST -> 2
        JewishDayKind.PURIM -> 3
        JewishDayKind.CHANUKAH -> 4
        JewishDayKind.EREV -> 5
        JewishDayKind.MODERN -> 6
        JewishDayKind.MINOR -> 7
        JewishDayKind.SPECIAL_SHABBAT -> 8
        JewishDayKind.ROSH_CHODESH -> 9
        JewishDayKind.PARASHA -> 10
    }
}
