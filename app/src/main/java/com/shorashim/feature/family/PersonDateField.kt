package com.shorashim.feature.family

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.core.common.HebrewDates
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.HebrewCalendarMath
import com.shorashim.core.ui.component.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

private val HEBREW_LOCALE: Locale = Locale.forLanguageTag("he-IL")
private val MIN_GREGORIAN_YEAR = 1600

/**
 * One date (birth or death) with a precision selector, a Gregorian/Hebrew switch and a tap-to-pick field.
 *
 * - Gregorian + full date: Android's Material calendar picker (tap the year to jump, or use its keyboard input).
 * - Everything else (Hebrew calendar, or month/year-only precision): a wheel-style dialog whose Hebrew columns show
 *   day / month / year as letters (כ״ב, תשרי, תשפ״ה).
 * The field always shows the date in both calendars.
 */
@Composable
fun DateInputSection(
    sectionTitle: String,
    precision: DatePrecision,
    onPrecisionChange: (DatePrecision) -> Unit,
    calendarType: CalendarInputType,
    onCalendarTypeChange: (CalendarInputType) -> Unit,
    year: String,
    month: String,
    day: String,
    onDateChosen: (year: Int, month: Int?, day: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    val chosen: LocalDate? = remember(year, month, day, precision) {
        val y = year.toIntOrNull() ?: return@remember null
        when (precision) {
            DatePrecision.UNKNOWN -> null
            DatePrecision.YEAR_ONLY -> runCatching { LocalDate.of(y, 7, 1) }.getOrNull()
            DatePrecision.YEAR_MONTH -> {
                val m = month.toIntOrNull() ?: return@remember null
                runCatching { LocalDate.of(y, m, 1) }.getOrNull()
            }
            DatePrecision.FULL_DATE -> {
                val m = month.toIntOrNull() ?: return@remember null
                val d = day.toIntOrNull() ?: return@remember null
                runCatching { LocalDate.of(y, m, d) }.getOrNull()
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(Spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            FilterChip(
                selected = precision == DatePrecision.FULL_DATE,
                onClick = { onPrecisionChange(DatePrecision.FULL_DATE) },
                label = { Text("מלא", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = precision == DatePrecision.YEAR_MONTH,
                onClick = { onPrecisionChange(DatePrecision.YEAR_MONTH) },
                label = { Text("חודש ושנה", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = precision == DatePrecision.YEAR_ONLY,
                onClick = { onPrecisionChange(DatePrecision.YEAR_ONLY) },
                label = { Text("שנה", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = precision == DatePrecision.UNKNOWN,
                onClick = { onPrecisionChange(DatePrecision.UNKNOWN) },
                label = { Text("לא ידוע", style = MaterialTheme.typography.labelSmall) },
            )
        }

        if (precision != DatePrecision.UNKNOWN) {
            Spacer(Modifier.height(Spacing.sm))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = calendarType == CalendarInputType.GREGORIAN,
                    onClick = { onCalendarTypeChange(CalendarInputType.GREGORIAN) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {},
                ) { Text("לועזי", style = MaterialTheme.typography.labelMedium) }
                SegmentedButton(
                    selected = calendarType == CalendarInputType.HEBREW,
                    onClick = { onCalendarTypeChange(CalendarInputType.HEBREW) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {},
                ) { Text("עברי", style = MaterialTheme.typography.labelMedium) }
            }

            Spacer(Modifier.height(Spacing.sm))

            val gregorianText = remember(chosen, precision) { gregorianLabel(chosen, precision) }
            val hebrewText = remember(chosen, precision) { hebrewLabel(chosen, precision) }
            val primary = if (calendarType == CalendarInputType.HEBREW) hebrewText else gregorianText
            val secondary = if (calendarType == CalendarInputType.HEBREW) gregorianText else hebrewText

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showPicker = true },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (chosen == null) {
                            Text(
                                text = "לחצו לבחירת תאריך",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = primary,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = secondary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        val useMaterialCalendar = calendarType == CalendarInputType.GREGORIAN && precision == DatePrecision.FULL_DATE
        if (useMaterialCalendar) {
            GregorianDatePickerDialog(
                initial = chosen,
                onConfirm = { date ->
                    showPicker = false
                    onDateChosen(date.year, date.monthValue, date.dayOfMonth)
                },
                onDismiss = { showPicker = false },
            )
        } else {
            WheelDateDialog(
                calendarType = calendarType,
                precision = precision,
                initial = chosen,
                onConfirm = { date ->
                    showPicker = false
                    onDateChosen(
                        date.year,
                        if (precision == DatePrecision.YEAR_ONLY) null else date.monthValue,
                        if (precision == DatePrecision.FULL_DATE) date.dayOfMonth else null,
                    )
                },
                onDismiss = { showPicker = false },
            )
        }
    }
}

private fun gregorianLabel(date: LocalDate?, precision: DatePrecision): String {
    if (date == null) return ""
    val monthName = date.month.getDisplayName(TextStyle.FULL, HEBREW_LOCALE)
    return when (precision) {
        DatePrecision.FULL_DATE -> "${date.dayOfMonth} ב$monthName ${date.year}"
        DatePrecision.YEAR_MONTH -> "$monthName ${date.year}"
        DatePrecision.YEAR_ONLY -> date.year.toString()
        DatePrecision.UNKNOWN -> ""
    }
}

private fun hebrewLabel(date: LocalDate?, precision: DatePrecision): String {
    if (date == null) return ""
    val h = HebrewCalendarMath.fromLocalDate(date)
    val yearLabel = HebrewDates.formatHebrewYear(h.year, includeThousands = false)
    val monthName = HebrewCalendarMath.monthName(h.year, h.month)
    return when (precision) {
        DatePrecision.FULL_DATE -> "${HebrewDates.formatHebrewDay(h.day)} ב$monthName $yearLabel"
        DatePrecision.YEAR_MONTH -> "$monthName $yearLabel"
        DatePrecision.YEAR_ONLY -> "שנת $yearLabel"
        DatePrecision.UNKNOWN -> ""
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Gregorian: Android's Material calendar
// ---------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GregorianDatePickerDialog(
    initial: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val startDate = initial ?: today
    val state = rememberDatePickerState(
        initialSelectedDateMillis = startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        yearRange = MIN_GREGORIAN_YEAR..today.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() <= today

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) { Text("אישור", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
    ) {
        DatePicker(state = state)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Wheel dialog (Hebrew calendar, and month/year-only precision for either calendar)
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun WheelDateDialog(
    calendarType: CalendarInputType,
    precision: DatePrecision,
    initial: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val start = initial ?: today
    val hebrew = calendarType == CalendarInputType.HEBREW

    val startHebrew = remember(start) { HebrewCalendarMath.fromLocalDate(start) }
    var year by remember { mutableIntStateOf(if (hebrew) startHebrew.year else start.year) }
    var month by remember { mutableIntStateOf(if (hebrew) startHebrew.month else start.monthValue) }
    var day by remember { mutableIntStateOf(if (hebrew) startHebrew.day else start.dayOfMonth) }

    val years: List<Int> = remember(hebrew) {
        if (hebrew) {
            val current = HebrewCalendarMath.fromLocalDate(today).year
            (current downTo 5400).toList()
        } else {
            (today.year downTo MIN_GREGORIAN_YEAR).toList()
        }
    }
    val monthOptions: List<Pair<Int, String>> = remember(hebrew, year) {
        if (hebrew) {
            HebrewCalendarMath.monthsOf(year).map { it to HebrewCalendarMath.monthName(year, it) }
        } else {
            (1..12).map { it to Month.of(it).getDisplayName(TextStyle.FULL, HEBREW_LOCALE) }
        }
    }
    // A leap-only month (Adar I) vanishes in a common year; fall back to the regular Adar.
    if (monthOptions.none { it.first == month }) {
        month = if (hebrew) HebrewCalendarMath.ADAR else monthOptions.last().first
    }
    val daysInMonth = if (hebrew) {
        HebrewCalendarMath.monthLength(year, month)
    } else {
        runCatching { java.time.YearMonth.of(year, month).lengthOfMonth() }.getOrDefault(31)
    }
    if (day > daysInMonth) day = daysInMonth

    val summary = run {
        val date = buildDate(hebrew, year, month, day, precision)
        when {
            date == null -> ""
            hebrew -> hebrewLabel(date, precision)
            else -> gregorianLabel(date, precision)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (hebrew) "בחירת תאריך עברי" else "בחירת תאריך",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (precision == DatePrecision.FULL_DATE) {
                    WheelColumn(
                        items = (1..daysInMonth).toList(),
                        selected = day,
                        label = { if (hebrew) HebrewDates.formatHebrewDay(it) else it.toString() },
                        onSelect = { day = it },
                        modifier = Modifier.weight(0.8f),
                    )
                }
                if (precision != DatePrecision.YEAR_ONLY) {
                    WheelColumn(
                        items = monthOptions.map { it.first },
                        selected = month,
                        label = { id -> monthOptions.first { it.first == id }.second },
                        onSelect = { month = it },
                        modifier = Modifier.weight(1.3f),
                    )
                }
                WheelColumn(
                    items = years,
                    selected = year,
                    label = { if (hebrew) HebrewDates.formatHebrewYear(it, includeThousands = false) else it.toString() },
                    onSelect = { year = it },
                    modifier = Modifier.weight(1.1f),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { buildDate(hebrew, year, month, day, precision)?.let(onConfirm) },
            ) { Text("אישור", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
    )
}

private fun buildDate(hebrew: Boolean, year: Int, month: Int, day: Int, precision: DatePrecision): LocalDate? {
    val effectiveDay = if (precision == DatePrecision.FULL_DATE) day else 1
    val effectiveMonth = if (precision == DatePrecision.YEAR_ONLY) {
        if (hebrew) HebrewCalendarMath.TISHREI else 1
    } else {
        month
    }
    return runCatching {
        if (hebrew) {
            HebrewCalendarMath.toLocalDate(year, effectiveMonth, effectiveDay)
        } else {
            LocalDate.of(year, effectiveMonth, effectiveDay)
        }
    }.getOrNull()
}

@Composable
private fun WheelColumn(
    items: List<Int>,
    selected: Int,
    label: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val selectedIndex = items.indexOf(selected).coerceAtLeast(0)
    LaunchedEffect(items.size) {
        listState.scrollToItem((selectedIndex - 2).coerceAtLeast(0))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(items) { _, value ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSelect(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(value),
                        fontSize = if (isSelected) 18.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
