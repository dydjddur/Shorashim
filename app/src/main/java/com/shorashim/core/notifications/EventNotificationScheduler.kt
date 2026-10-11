package com.shorashim.core.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.shorashim.core.model.EventDateCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class EventNotificationScheduler(
    private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        const val CHANNEL_ID = "shorashim_events_channel"
        const val EXTRA_TITLE = "extra_notification_title"
        const val EXTRA_MESSAGE = "extra_notification_message"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_TYPE = "extra_event_type"

        const val TYPE_MEMORIAL = "MEMORIAL"
        const val TYPE_BIRTHDAY = "BIRTHDAY"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "תזכורות אירועים"
            val descriptionText = "התראות ימי הולדת ואזכרות משפחתיות"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules a memorial reminder exactly 7 days before the event at 09:00 AM Israel time.
     * If fewer than 7 days remain, or if the 7-day mark has already passed, does not schedule.
     */
    fun scheduleMemorialReminder(
        eventId: String,
        personName: String,
        targetDate: LocalDate,
    ) {
        val today = EventDateCalculator.getTodayInIsrael()
        var reminderDate = targetDate.minusDays(7)
        val nowIsrael = ZonedDateTime.now(EventDateCalculator.ISRAEL_ZONE)
        val triggerToday = ZonedDateTime.of(today, LocalTime.of(9, 0), EventDateCalculator.ISRAEL_ZONE)

        if (reminderDate.isBefore(today) || (reminderDate == today && nowIsrael.isAfter(triggerToday))) {
            reminderDate = targetDate.plusYears(1).minusDays(7)
        }

        val triggerTimeEpochMs = ZonedDateTime.of(
            reminderDate,
            LocalTime.of(9, 0),
            EventDateCalculator.ISRAEL_ZONE,
        ).toInstant().toEpochMilli()

        val title = "תזכורת לאזכרה משפחתית"
        val message = if (personName.isNotBlank()) {
            "תזכורת לאזכרה: בעוד שבוע נציין את זכרו של $personName. זה הזמן להתכונן, לומר קדיש ולקרוא תהילים לעילוי נשמתו."
        } else {
            "תזכורת לאזכרה: בעוד שבוע נציין אזכרה משפחתית. זה הזמן להתכונן, לומר קדיש ולקרוא תהילים לעילוי הנשמה."
        }

        val requestCode = (eventId.hashCode() and 0x7FFFFFFF)
        scheduleAlarm(requestCode, triggerTimeEpochMs, title, message, eventId, TYPE_MEMORIAL)
    }

    /**
     * Schedules a birthday notification on the birthday itself at 09:00 AM Israel time.
     */
    fun scheduleBirthdayNotification(
        personId: String,
        personName: String,
        targetDate: LocalDate,
    ) {
        val today = EventDateCalculator.getTodayInIsrael()
        if (targetDate.isBefore(today)) return

        val triggerTimeEpochMs = ZonedDateTime.of(
            targetDate,
            LocalTime.of(9, 0),
            EventDateCalculator.ISRAEL_ZONE,
        ).toInstant().toEpochMilli()

        val title = "יום הולדת שמח! 🎉"
        val message = EventDateCalculator.getBirthdayMessage(personName, isToday = true)

        val requestCode = (personId.hashCode() and 0x7FFFFFFF) xor 0x123456
        scheduleAlarm(requestCode, triggerTimeEpochMs, title, message, personId, TYPE_BIRTHDAY)
    }

    /**
     * Cancels an alarm for a memorial event.
     */
    fun cancelMemorialReminder(eventId: String) {
        val requestCode = (eventId.hashCode() and 0x7FFFFFFF)
        cancelAlarm(requestCode)
    }

    private fun scheduleAlarm(
        requestCode: Int,
        triggerTimeEpochMs: Long,
        title: String,
        message: String,
        eventId: String,
        eventType: String,
    ) {
        if (alarmManager == null) return

        val intent = Intent(context, EventReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TYPE, eventType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeEpochMs,
                    pendingIntent,
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeEpochMs,
                    pendingIntent,
                )
            }
            Log.d("EventScheduler", "Alarm scheduled for event $eventId at epoch $triggerTimeEpochMs")
        } catch (e: SecurityException) {
            Log.w("EventScheduler", "Exact alarm permission not granted, falling back to setWindow", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeEpochMs,
                pendingIntent,
            )
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        if (alarmManager == null) return
        val intent = Intent(context, EventReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
