package com.shorashim.feature.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.R
import com.shorashim.core.common.HebrewDates
import com.shorashim.core.model.CalendarEvent
import com.shorashim.core.model.EventCalendarMath
import com.shorashim.core.model.EventDateCalculator
import com.shorashim.core.model.JewishCalendar
import com.shorashim.core.model.JewishDay
import com.shorashim.core.model.JewishDayKind
import com.shorashim.core.ui.component.Spacing
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val HEBREW_LOCALE: Locale = Locale.forLanguageTag("he-IL")
private val WEEKDAY_NAMES = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")
private const val MAX_CHIPS_PER_DAY = 2
private const val SWIPE_THRESHOLD_PX = 90f

private val BirthdayColor = Color(0xFFD81B60)
private val MemorialColor = Color(0xFFE08A00)

/** Palette of the month grid; derived from the reference design (teal header, tinted Saturday column). */
private data class CalendarPalette(
    val header: Color,
    val onHeader: Color,
    val gridLine: Color,
    val saturdayTint: Color,
    val outsideMonthNumber: Color,
    val dayNumber: Color,
    val hebrewDay: Color,
    val today: Color,
    val yomTovTint: Color,
    val yomTovText: Color,
    val fastText: Color,
    val jewishText: Color,
)

@Composable
private fun rememberCalendarPalette(): CalendarPalette {
    val dark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    return remember(dark, scheme) {
        if (dark) {
            CalendarPalette(
                header = Color(0xFF1B5242),
                onHeader = Color.White,
                gridLine = Color(0xFF2F5B4B).copy(alpha = 0.7f),
                saturdayTint = Color(0xFF14281F),
                outsideMonthNumber = Color(0xFF5E7C6C),
                dayNumber = scheme.onSurface,
                hebrewDay = Color(0xFF9DBBAA),
                today = Color(0xFF48A887),
                yomTovTint = Color(0xFF2B2713),
                yomTovText = Color(0xFFFFC857),
                fastText = Color(0xFFFFB4AB),
                jewishText = Color(0xFF8FD9BB),
            )
        } else {
            CalendarPalette(
                header = Color(0xFF1F7A5C),
                onHeader = Color.White,
                gridLine = Color(0xFF1F7A5C).copy(alpha = 0.45f),
                saturdayTint = Color(0xFFD7EBDD),
                outsideMonthNumber = Color(0xFF8AA396),
                dayNumber = Color(0xFF1B1B1B),
                hebrewDay = Color(0xFF506256),
                today = Color(0xFF1B5242),
                yomTovTint = Color(0xFFFFF1CC),
                yomTovText = Color(0xFF9A5B00),
                fastText = Color(0xFFB3261E),
                jewishText = Color(0xFF1B5242),
            )
        }
    }
}

/**
 * Month-grid calendar of [events] (birthdays + memorials). Sunday is the first column, the Saturday
 * column is tinted, every cell shows the Gregorian day (large) and the Hebrew day (small).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsCalendarView(
    events: List<CalendarEvent>,
    onMemorialClick: (CalendarEvent.Memorial) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = rememberCalendarPalette()
    val today = remember { EventDateCalculator.getTodayInIsrael() }

    // Month index = year * 12 + (month - 1) so it survives configuration changes.
    var monthIndex by rememberSaveable { mutableIntStateOf(today.year * 12 + today.monthValue - 1) }
    val month by remember { derivedStateOf { YearMonth.of(monthIndex / 12, monthIndex % 12 + 1) } }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    val grid = remember(month) { EventCalendarMath.monthGrid(month) }
    val eventsByDay = remember(month, events) {
        grid.associateWith { EventCalendarMath.eventsOn(events, it) }
    }
    val jewishByDay = remember(month) { grid.associateWith { JewishCalendar.daysFor(it) } }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    fun shiftMonth(delta: Int) {
        monthIndex += delta
    }

    Column(modifier = modifier.fillMaxSize()) {
        CalendarHeader(
            month = month,
            palette = palette,
            isCurrentMonth = month == YearMonth.from(today),
            onPrevious = { shiftMonth(-1) },
            onNext = { shiftMonth(1) },
            onToday = { monthIndex = today.year * 12 + today.monthValue - 1 },
        )

        WeekdayHeader(palette)

        var dragTotal by remember { mutableStateOf(0f) }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(isRtl) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onDragEnd = {
                            if (abs(dragTotal) > SWIPE_THRESHOLD_PX) {
                                // Dragging toward the end side of the screen moves forward in time.
                                val forward = if (isRtl) dragTotal > 0 else dragTotal < 0
                                shiftMonth(if (forward) 1 else -1)
                            }
                            dragTotal = 0f
                        },
                        onDragCancel = { dragTotal = 0f },
                    ) { _, delta -> dragTotal += delta }
                }
                .border(0.5.dp, palette.gridLine),
        ) {
            grid.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            inMonth = YearMonth.from(date) == month,
                            isToday = date == today,
                            events = eventsByDay[date].orEmpty(),
                            jewishDays = jewishByDay[date].orEmpty(),
                            palette = palette,
                            onClick = { selectedDay = date },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }

        CalendarLegend()
    }

    selectedDay?.let { day ->
        val dayEvents = eventsByDay[day].orEmpty()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedDay = null },
            sheetState = sheetState,
        ) {
            DayEventsSheetContent(
                date = day,
                events = dayEvents,
                jewishDays = jewishByDay[day].orEmpty(),
                onMemorialClick = {
                    selectedDay = null
                    onMemorialClick(it)
                },
            )
        }
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    palette: CalendarPalette,
    isCurrentMonth: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val monthName = remember(month) { month.month.getDisplayName(TextStyle.FULL, HEBREW_LOCALE) }
    val hebrewRange = remember(month) { HebrewDates.rangeLabel(month.atDay(1), month.atEndOfMonth()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = monthName,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = month.year.toString(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
            }
            Text(
                text = hebrewRange,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.hebrewDay,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!isCurrentMonth) {
            TextButton(onClick = onToday) {
                Text(stringResource(R.string.events_calendar_today), color = palette.today, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.events_calendar_previous_month),
                tint = palette.today,
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(R.string.events_calendar_next_month),
                tint = palette.today,
            )
        }
    }
}

@Composable
private fun WeekdayHeader(palette: CalendarPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.header),
    ) {
        WEEKDAY_NAMES.forEach { name ->
            Text(
                text = name,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                color = palette.onHeader,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    events: List<CalendarEvent>,
    jewishDays: List<JewishDay>,
    palette: CalendarPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hebrewLabel = remember(date) {
        HebrewDates.dayLabel(date)
    }
    val contentAlpha = if (inMonth) 1f else 0.55f

    Box(
        modifier = modifier
            .background(
                when {
                    jewishDays.any { it.isYomTov } -> palette.yomTovTint
                    EventCalendarMath.isSaturday(date) -> palette.saturdayTint
                    else -> Color.Transparent
                },
            )
            .border(0.5.dp, palette.gridLine)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 3.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clipToBounds(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val shownJewish = jewishDays
                    .filter { it.kind != JewishDayKind.ROSH_CHODESH || jewishDays.size == 1 }
                    .take(2)
                shownJewish.forEach { day ->
                    Text(
                        text = day.title,
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        fontWeight = if (day.isYomTov) FontWeight.ExtraBold else FontWeight.Bold,
                        color = jewishColor(day, palette).copy(alpha = contentAlpha),
                        maxLines = if (day.isYomTov) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1.dp),
                    )
                }
                events.take(MAX_CHIPS_PER_DAY).forEach { event -> EventChip(event, contentAlpha) }
                if (events.size > MAX_CHIPS_PER_DAY) {
                    Text(
                        text = "+${events.size - MAX_CHIPS_PER_DAY}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.hebrewDay,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
            }

            Spacer(Modifier.weight(0.01f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = hebrewLabel,
                    fontSize = if (hebrewLabel.length > 3) 8.sp else 10.sp,
                    color = palette.hebrewDay.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = if (isToday) {
                        Modifier
                            .clip(CircleShape)
                            .background(palette.today)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    } else {
                        Modifier
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isToday -> Color.White
                            inMonth -> palette.dayNumber
                            else -> palette.outsideMonthNumber
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventChip(event: CalendarEvent, alpha: Float) {
    val color = if (event is CalendarEvent.Birthday) BirthdayColor else MemorialColor
    Text(
        text = event.personName.substringBefore(' '),
        fontSize = 9.sp,
        lineHeight = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color.copy(alpha = alpha),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.16f * alpha))
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(BirthdayColor, stringResource(R.string.events_birthday_tag))
        LegendItem(MemorialColor, stringResource(R.string.events_memorial_tag))
        LegendItem(Color(0xFFD9A400), stringResource(R.string.events_holiday_tag))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayEventsSheetContent(
    date: LocalDate,
    events: List<CalendarEvent>,
    jewishDays: List<JewishDay>,
    onMemorialClick: (CalendarEvent.Memorial) -> Unit,
) {
    val palette = rememberCalendarPalette()
    val monthName = remember(date) { date.month.hebrewName() }
    val hebrewDate = remember(date) {
        "${HebrewDates.dayLabel(date)} ${HebrewDates.monthName(date)} ה'${HebrewDates.yearLabel(date)}"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = "${date.dayOfMonth} ב$monthName ${date.year}",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = hebrewDate,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        jewishDays.forEach { day ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(jewishColor(day, palette)),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = day.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (events.isEmpty() && jewishDays.isEmpty()) {
            Text(
                text = stringResource(R.string.events_calendar_empty_day),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        events.forEach { event ->
            when (event) {
                is CalendarEvent.Birthday -> BirthdayCard(event = event)
                is CalendarEvent.Memorial -> MemorialCard(event = event, onClick = { onMemorialClick(event) })
            }
        }
    }
}

private fun Month.hebrewName(): String = getDisplayName(TextStyle.FULL, HEBREW_LOCALE)

private fun jewishColor(day: JewishDay, palette: CalendarPalette): Color = when (day.kind) {
    JewishDayKind.YOM_TOV -> palette.yomTovText
    JewishDayKind.FAST -> palette.fastText
    JewishDayKind.CHOL_HAMOED, JewishDayKind.CHANUKAH, JewishDayKind.PURIM -> palette.today
    else -> palette.jewishText
}
