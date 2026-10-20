package com.shorashim.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android deletes all alarms on reboot (and when the app is updated). This re-runs the background sync right away so
 * birthday / memorial reminders are scheduled again. (It used to only log a message, so reminders silently stopped
 * after every restart.)
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ChatNotificationWorker.runNow(context, forceEventSync = true)
        }
    }
}
