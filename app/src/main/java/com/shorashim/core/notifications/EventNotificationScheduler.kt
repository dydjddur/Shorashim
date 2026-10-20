package com.shorashim.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.shorashim.MainActivity
import com.shorashim.R
import com.shorashim.core.model.EventDateCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Birthday and memorial (azkara) reminders at 09:00 Israel time:
 * - birthday: on the day itself;
 * - memorial: 7 days before (to prepare) and on the day itself.
 *
 * Fixes compared to the first version:
 * - Uses exact alarms when allowed and `setAndAllowWhileIdle` otherwise (the old fallback, plain `set`, could be
 *   deferred for hours by Doze, so reminders arrived late or not at all).
 * - If the app only learns about today's event after 09:00 (phone was off, app was not opened), the reminder is
 *   posted right away instead of being skipped.
 * - Every reminder is shown at most once per date (re-scheduling on each refresh never re-posts it).
 */
class EventNotificationScheduler(
    private val context: Context,
    /** Current time in Israel; injectable so device tests can simulate any time of day. */
    private val now: () -> ZonedDateTime = { ZonedDateTime.now(EventDateCalculator.ISRAEL_ZONE) },
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    companion object {
        const val CHANNEL_ID = "shorashim_events_channel"
        const val EXTRA_TITLE = "extra_notification_title"
        const val EXTRA_MESSAGE = "extra_notification_message"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_TYPE = "extra_event_type"
        const val EXTRA_DEDUPE_KEY = "extra_dedupe_key"

        const val TYPE_MEMORIAL = "MEMORIAL"
        const val TYPE_MEMORIAL_WEEK = "MEMORIAL_WEEK"
        const val TYPE_BIRTHDAY = "BIRTHDAY"

        private const val PREFS = "shorashim_event_reminders"
        private const val TAG = "EventScheduler"
        private val REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, "תזכורות אירועים", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "התראות ימי הולדת ואזכרות משפחתיות"
                    enableVibration(true)
                }
                context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            }
        }

        /** Posts a reminder notification and records [dedupeKey] so it is never shown twice. */
        fun postNotification(context: Context, title: String, message: String, eventId: String, dedupeKey: String?) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (dedupeKey != null && prefs.getBoolean(dedupeKey, false)) return
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Notification permission not granted; reminder '$title' not shown")
                return
            }
            ensureChannel(context)
            val notificationId = ((dedupeKey ?: eventId).hashCode() and 0x3FFFFFFF)
            val openApp = PendingIntent.getActivity(
                context,
                notificationId,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()
            runCatching { NotificationManagerCompat.from(context).notify(notificationId, notification) }
                .onSuccess { if (dedupeKey != null) prefs.edit().putBoolean(dedupeKey, true).apply() }
                .onFailure { Log.w(TAG, "Could not post reminder: ${it.message}") }
        }
    }

    init {
        ensureChannel(context)
    }

    /** Memorial: reminder one week before and on the day itself, at 09:00 Israel time. */
    fun scheduleMemorialReminder(
        eventId: String,
        personName: String,
        targetDate: LocalDate,
    ) {
        val name = personName.ifBlank { null }
        scheduleOrCatchUp(
            date = targetDate.minusDays(7),
            type = TYPE_MEMORIAL_WEEK,
            id = eventId,
            title = "בעוד שבוע: אזכרה${name?.let { " ל$it" }.orEmpty()}",
            message = if (name != null) {
                "בעוד שבוע נציין את זכרו/ה של $name. זה הזמן להתכונן, לומר קדיש ולקרוא תהילים לעילוי נשמתו/ה."
            } else {
                "בעוד שבוע נציין אזכרה משפחתית. זה הזמן להתכונן, לומר קדיש ולקרוא תהילים לעילוי הנשמה."
            },
            catchUpWindowDays = 3,
        )
        scheduleOrCatchUp(
            date = targetDate,
            type = TYPE_MEMORIAL,
            id = eventId,
            title = "היום האזכרה${name?.let { " של $it" }.orEmpty()} 🕯️",
            message = "היום יום הזיכרון${name?.let { " של $it" }.orEmpty()}. אפשר לקרוא תהילים לעילוי נשמתו/ה מתוך האפליקציה.",
        )
    }

    /** Birthday: on the day itself at 09:00 Israel time. */
    fun scheduleBirthdayNotification(
        personId: String,
        personName: String,
        targetDate: LocalDate,
    ) {
        scheduleOrCatchUp(
            date = targetDate,
            type = TYPE_BIRTHDAY,
            id = personId,
            title = "יום הולדת שמח! 🎉",
            message = EventDateCalculator.getBirthdayMessage(personName, isToday = true),
        )
    }

    /** Cancels both alarms of a deleted memorial. */
    fun cancelMemorialReminder(eventId: String) {
        cancelAlarm(requestCode(TYPE_MEMORIAL, eventId))
        cancelAlarm(requestCode(TYPE_MEMORIAL_WEEK, eventId))
    }

    /**
     * Schedules the 09:00 alarm for [date], or - when that moment already passed but is at most [catchUpWindowDays]
     * days ago - shows the reminder now (once).
     */
    private fun scheduleOrCatchUp(
        date: LocalDate,
        type: String,
        id: String,
        title: String,
        message: String,
        catchUpWindowDays: Long = 0,
    ) {
        val current = now()
        val trigger = ZonedDateTime.of(date, REMINDER_TIME, EventDateCalculator.ISRAEL_ZONE)
        val dedupeKey = "$type|$id|$date"
        if (prefs.getBoolean(dedupeKey, false)) return

        if (trigger.isAfter(current)) {
            scheduleAlarm(requestCode(type, id), trigger.toInstant().toEpochMilli(), title, message, id, type, dedupeKey)
        } else if (!date.isBefore(current.toLocalDate().minusDays(catchUpWindowDays))) {
            postNotification(context, title, message, id, dedupeKey)
        }
    }

    private fun requestCode(type: String, id: String): Int = ("$type|$id".hashCode() and 0x7FFFFFFF)

    /** Schedules a reminder at an exact epoch time. Internal so device tests can verify a real alarm firing. */
    internal fun scheduleAlarm(
        requestCode: Int,
        triggerTimeEpochMs: Long,
        title: String,
        message: String,
        eventId: String,
        eventType: String,
        dedupeKey: String?,
    ) {
        val am = alarmManager ?: return
        val intent = Intent(context, EventReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TYPE, eventType)
            putExtra(EXTRA_DEDUPE_KEY, dedupeKey)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeEpochMs, pendingIntent)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeEpochMs, pendingIntent)
            }
            Log.d(TAG, "Reminder $eventType/$eventId scheduled for $triggerTimeEpochMs (exact=$canExact)")
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeEpochMs, pendingIntent)
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        val am = alarmManager ?: return
        val intent = Intent(context, EventReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            am.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
