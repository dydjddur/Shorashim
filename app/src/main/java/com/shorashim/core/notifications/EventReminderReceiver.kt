package com.shorashim.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by AlarmManager at 09:00 on the reminder day; posts the notification (once per event and date). */
class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EventNotificationScheduler.EXTRA_TITLE) ?: "תזכורת משורשים"
        val message = intent.getStringExtra(EventNotificationScheduler.EXTRA_MESSAGE) ?: ""
        val eventId = intent.getStringExtra(EventNotificationScheduler.EXTRA_EVENT_ID) ?: ""
        val dedupeKey = intent.getStringExtra(EventNotificationScheduler.EXTRA_DEDUPE_KEY)
        EventNotificationScheduler.postNotification(context, title, message, eventId, dedupeKey)
    }
}
