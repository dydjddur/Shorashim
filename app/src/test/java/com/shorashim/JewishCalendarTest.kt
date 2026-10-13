package com.shorashim

import com.shorashim.core.common.HebrewDates
import com.shorashim.core.model.HebrewCalendarMath
import com.shorashim.core.model.JewishCalendar
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cross-checks the pure-Kotlin Hebrew calendar against Hebcal (Israel) golden data for 2024-2036. */
class JewishCalendarTest {

    private val english = listOf(
        "Bereshit", "Noach", "Lech-Lecha", "Vayera", "Chayei Sara", "Toldot", "Vayetzei", "Vayishlach",
        "Vayeshev", "Miketz", "Vayigash", "Vayechi", "Shemot", "Vaera", "Bo", "Beshalach", "Yitro",
        "Mishpatim", "Terumah", "Tetzaveh", "Ki Tisa", "Vayakhel", "Pekudei", "Vayikra", "Tzav", "Shmini",
        "Tazria", "Metzora", "Achrei Mot", "Kedoshim", "Emor", "Behar", "Bechukotai", "Bamidbar", "Nasso",
        "Beha'alotcha", "Sh'lach", "Korach", "Chukat", "Balak", "Pinchas", "Matot", "Masei", "Devarim",
        "Vaetchanan", "Eikev", "Re'eh", "Shoftim", "Ki Teitzei", "Ki Tavo", "Nitzavim", "Vayeilech", "Ha'azinu",
    )

    private val golden: List<Triple<LocalDate, String, String>> by lazy {
        javaClass.getResourceAsStream("/hebcal_israel_2024_2036.txt")!!.bufferedReader(Charsets.UTF_8).readLines()
            .filter { it.isNotBlank() }
            .map { line ->
                val p = line.split("|")
                Triple(LocalDate.parse(p[0]), p[1], p.getOrElse(2) { "" })
            }
    }

    /** Names may contain a hyphen themselves (Lech-Lecha), so match against known single/double spellings. */
    private fun hebrewParasha(englishName: String): String {
        for (i in english.indices) {
            if (englishName == english[i]) return JewishCalendar.PARASHOT[i]
            if (i + 1 < english.size && englishName == "${english[i]}-${english[i + 1]}") {
                return "${JewishCalendar.PARASHOT[i]}-${JewishCalendar.PARASHOT[i + 1]}"
            }
        }
        error("unknown parasha $englishName")
    }

    private val titleToId = mapOf(
        "Rosh Hashana I" to "rosh_hashana_1",
        "Rosh Hashana II" to "rosh_hashana_2",
        "Tzom Gedaliah" to "tzom_gedaliah",
        "Erev Yom Kippur" to "erev_yom_kippur",
        "Yom Kippur" to "yom_kippur",
        "Erev Sukkot" to "erev_sukkot",
        "Sukkot I" to "sukkot_1",
        "Sukkot VII (Hoshana Raba)" to "hoshana_raba",
        "Shmini Atzeret" to "shmini_atzeret",
        "Asara B'Tevet" to "asara_btevet",
        "Tu BiShvat" to "tu_bishvat",
        "Ta'anit Esther" to "taanit_esther",
        "Erev Purim" to "erev_purim",
        "Purim" to "purim",
        "Shushan Purim" to "shushan_purim",
        "Purim Katan" to "purim_katan",
        "Shushan Purim Katan" to "shushan_purim_katan",
        "Erev Pesach" to "erev_pesach",
        "Ta'anit Bechorot" to "taanit_bechorot",
        "Pesach I" to "pesach_1",
        "Pesach VII" to "pesach_7",
        "Yom HaShoah" to "yom_hashoah",
        "Yom HaZikaron" to "yom_hazikaron",
        "Yom HaAtzma'ut" to "yom_haatzmaut",
        "Pesach Sheni" to "pesach_sheni",
        "Lag BaOmer" to "lag_baomer",
        "Yom Yerushalayim" to "yom_yerushalayim",
        "Erev Shavuot" to "erev_shavuot",
        "Shavuot" to "shavuot",
        "Tzom Tammuz" to "tzom_tammuz",
        "Erev Tish'a B'Av" to "erev_tisha_bav",
        "Tish'a B'Av" to "tisha_bav",
        "Tish'a B'Av (observed)" to "tisha_bav",
        "Tu B'Av" to "tu_bav",
        "Erev Rosh Hashana" to "erev_rosh_hashana",
        "Sigd" to "sigd",
        "Shabbat Shuva" to "shabbat_shuva",
        "Shabbat Shirah" to "shabbat_shirah",
        "Shabbat Shekalim" to "shabbat_shekalim",
        "Shabbat Zachor" to "shabbat_zachor",
        "Shabbat Parah" to "shabbat_parah",
        "Shabbat HaChodesh" to "shabbat_hachodesh",
        "Shabbat HaGadol" to "shabbat_hagadol",
        "Shabbat Chazon" to "shabbat_chazon",
        "Shabbat Nachamu" to "shabbat_nachamu",
    )

    private fun idFor(title: String): String = titleToId[title] ?: when {
        title.startsWith("Sukkot ") && title.endsWith("(CH''M)") -> "sukkot_chm"
        title.startsWith("Pesach ") && title.endsWith("(CH''M)") -> "pesach_chm"
        else -> error("unmapped title $title")
    }

    private fun normalise(id: String) = id.replace(Regex("(sukkot_chm|pesach_chm)_\\d+"), "$1")

    @Test
    fun `hebrew dates match the reference September 2025 calendar`() {
        assertEquals(LocalDate.of(2025, 9, 23), HebrewCalendarMath.roshHashana(5786))
        val shabbatKiTeitzei = HebrewCalendarMath.fromLocalDate(LocalDate.of(2025, 9, 6))
        assertEquals(HebrewCalendarMath.ELUL, shabbatKiTeitzei.month)
        assertEquals(13, shabbatKiTeitzei.day)
        assertEquals("יג", HebrewDates.dayLabel(LocalDate.of(2025, 9, 6)))
        // 1 Sep 2025 is still Elul; 22 Sep is Erev Rosh Hashana (29 Elul)
        assertEquals("אלול", HebrewDates.monthName(LocalDate.of(2025, 9, 1)))
        assertEquals("תשרי", HebrewDates.monthName(LocalDate.of(2025, 9, 30)))
        assertEquals("אלול-תשרי", HebrewDates.rangeLabel(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30)).substringBefore(" "))
    }

    @Test
    fun `reference calendar September 2025 shows the right portions and holidays`() {
        assertEquals("כי תצא", JewishCalendar.parashaFor(LocalDate.of(2025, 9, 6)))
        assertEquals("כי תבוא", JewishCalendar.parashaFor(LocalDate.of(2025, 9, 13)))
        assertEquals("נצבים", JewishCalendar.parashaFor(LocalDate.of(2025, 9, 20)))
        assertEquals("וילך", JewishCalendar.parashaFor(LocalDate.of(2025, 9, 27)))
        assertEquals("האזינו", JewishCalendar.parashaFor(LocalDate.of(2025, 10, 4)))
        assertNull(JewishCalendar.parashaFor(LocalDate.of(2025, 9, 24))) // not a Saturday
        assertTrue(JewishCalendar.daysFor(LocalDate.of(2025, 9, 22)).any { it.id == "erev_rosh_hashana" })
        assertTrue(JewishCalendar.daysFor(LocalDate.of(2025, 9, 23)).any { it.id == "rosh_hashana_1" })
        assertTrue(JewishCalendar.daysFor(LocalDate.of(2025, 10, 2)).any { it.id == "yom_kippur" })
    }

    @Test
    fun `every weekly portion matches Hebcal`() {
        val rows = golden.filter { it.second == "parasha" }
        assertTrue(rows.size > 600)
        for ((date, _, name) in rows) {
            assertEquals("portion on $date", hebrewParasha(name), JewishCalendar.parashaFor(date))
        }
    }

    @Test
    fun `holidays fasts and special shabbatot match Hebcal in both directions`() {
        val expected = golden.filter { it.second == "holiday" }
            .groupBy({ it.first }, { normalise(idFor(it.third)) })
        val tracked = expected.values.flatten().toSet() + setOf("sukkot_chm", "pesach_chm", "taanit_bechorot")
        var d = LocalDate.of(2024, 10, 3)
        val last = LocalDate.of(2036, 9, 1)
        while (d.isBefore(last)) {
            val mine = JewishCalendar.daysFor(d).map { normalise(it.id) }.filter { it in tracked }.toSet()
            assertEquals("holidays on $d", expected[d].orEmpty().toSet(), mine)
            d = d.plusDays(1)
        }
    }

    @Test
    fun `rosh chodesh and chanukah match Hebcal`() {
        val rc = golden.filter { it.second == "rc" }.map { it.first }.toSet()
        var d = LocalDate.of(2024, 10, 3)
        while (d.isBefore(LocalDate.of(2036, 9, 1))) {
            val mine = JewishCalendar.daysFor(d).any { it.id == "rosh_chodesh" }
            assertEquals("rosh chodesh on $d", d in rc, mine)
            d = d.plusDays(1)
        }
        // Hebcal lists "N candles" on the eve; the Nth day of Chanukah is the date of "N+1 candles".
        for ((date, _, n) in golden.filter { it.second == "chanukah" }) {
            val c = n.toInt()
            if (c in 2..8) {
                assertTrue("day ${c - 1} of Chanukah on $date", JewishCalendar.daysFor(date).any { it.id == "chanukah_${c - 1}" })
            }
        }
    }

    @Test
    fun `gematria day labels`() {
        assertEquals("טו", HebrewDates.dayLabel(HebrewCalendarMath.toLocalDate(5786, HebrewCalendarMath.TISHREI, 15)))
        assertEquals("ל", HebrewDates.dayLabel(HebrewCalendarMath.toLocalDate(5786, HebrewCalendarMath.TISHREI, 30)))
    }

    @Test
    fun `formatHebrewDay and parseGematria`() {
        assertEquals("א׳", HebrewDates.formatHebrewDay(1))
        assertEquals("ט״ו", HebrewDates.formatHebrewDay(15))
        assertEquals("ט״ז", HebrewDates.formatHebrewDay(16))
        assertEquals("כ״ד", HebrewDates.formatHebrewDay(24))
        assertEquals("ל׳", HebrewDates.formatHebrewDay(30))

        assertEquals(1, HebrewDates.parseGematria("א"))
        assertEquals(1, HebrewDates.parseGematria("א׳"))
        assertEquals(15, HebrewDates.parseGematria("טו"))
        assertEquals(15, HebrewDates.parseGematria("ט״ו"))
        assertEquals(16, HebrewDates.parseGematria("טז"))
        assertEquals(24, HebrewDates.parseGematria("כ״ד"))
        assertEquals(5784, HebrewDates.parseGematria("תשפ״ד"))
        assertEquals(5784, HebrewDates.parseGematria("ה׳תשפ״ד"))
        assertEquals(5785, HebrewDates.parseGematria("תשפ״ה"))
    }

    @Test
    fun `formatHebrewYear`() {
        assertEquals("ה׳תשפ״ד", HebrewDates.formatHebrewYear(5784))
        assertEquals("ה׳תשפ״ו", HebrewDates.formatHebrewYear(5786))
        assertEquals("תשפ״ד", HebrewDates.formatHebrewYear(5784, includeThousands = false))
    }

    @Test
    fun `hebrew anniversary calculation`() {
        // Person died on 15 Nissan 5780 (Passover 1)
        val nissan15 = HebrewCalendarMath.toLocalDate(5780, HebrewCalendarMath.NISAN, 15)
        val anniv5784 = HebrewCalendarMath.getHebrewAnniversaryDate(nissan15, 5784)
        val annivHebrew = HebrewCalendarMath.fromLocalDate(anniv5784)

        assertEquals(5784, annivHebrew.year)
        assertEquals(HebrewCalendarMath.NISAN, annivHebrew.month)
        assertEquals(15, annivHebrew.day)
    }
}
