package com.shorashim.core.notifications

import android.content.Context
import android.util.Log
import com.shorashim.core.model.CalendarEvent
import com.shorashim.data.events.EventsRepository

/**
 * Schedules birthday / memorial reminders for all trees the user can see.
 *
 * Previously this only happened inside the Events tab, so a user who never opened that tab - or whose phone
 * rebooted (which wipes all alarms) - simply never got reminders. It now also runs on app start, from the periodic
 * background worker and after reboot / app update.
 */
object EventReminderSync {
    private const val PREFS = "shorashim_event_reminders_sync"
    private const val KEY_LAST_SYNC = "last_sync_ms"
    private const val MIN_INTERVAL_MS = 6 * 60 * 60 * 1000L

    suspend fun sync(
        context: Context,
        eventsRepository: EventsRepository,
        scheduler: EventNotificationScheduler,
        force: Boolean = false,
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong(KEY_LAST_SYNC, 0L) < MIN_INTERVAL_MS) return

        val events = eventsRepository.loadUpcomingEvents().getOrElse {
            Log.w("EventReminderSync", "Could not load events: ${it.message}")
            return
        }
        scheduleAll(scheduler, events)
        prefs.edit().putLong(KEY_LAST_SYNC, now).apply()
    }

    fun scheduleAll(scheduler: EventNotificationScheduler, events: List<CalendarEvent>) {
        for (event in events) {
            when (event) {
                is CalendarEvent.Birthday -> scheduler.scheduleBirthdayNotification(
                    personId = event.personId,
                    personName = event.personName,
                    targetDate = event.targetDate,
                )
                is CalendarEvent.Memorial -> scheduler.scheduleMemorialReminder(
                    eventId = event.memorialId,
                    personName = event.personName,
                    targetDate = event.targetDate,
                )
            }
        }
    }
}
