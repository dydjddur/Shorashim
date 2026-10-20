package com.shorashim.core.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shorashim.ShorashimApplication
import io.github.jan.supabase.auth.auth
import java.util.concurrent.TimeUnit

/**
 * Periodic background sync: new chat message notifications and birthday / memorial reminder alarms.
 *
 * Background check for new chat messages while the app is closed. Android runs periodic work at most every 15 minutes
 * (and may delay it further in battery saver / Doze), so this is a safety net next to the in-app checks.
 */
class ChatNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as? ShorashimApplication)?.container ?: return Result.success()
        val client = container.supabaseClient ?: return Result.success()
        // A fresh background process restores the saved session asynchronously.
        runCatching { client.auth.awaitInitialization() }
        if (client.auth.currentUserOrNull() == null) return Result.success()

        container.notificationScheduler?.let { scheduler ->
            runCatching {
                EventReminderSync.sync(
                    applicationContext,
                    container.eventsRepository,
                    scheduler,
                    force = inputData.getBoolean(KEY_FORCE_EVENT_SYNC, false),
                )
            }
        }
        return runCatching { ChatMessageNotifier.checkAndNotify(applicationContext, container.chatRepository) }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        private const val WORK_NAME = "shorashim_chat_message_check"
        private const val KEY_FORCE_EVENT_SYNC = "force_event_sync"

        /** One-off run, e.g. right after a reboot (which wipes all alarms). */
        fun runNow(context: Context, forceEventSync: Boolean) {
            val request = androidx.work.OneTimeWorkRequestBuilder<ChatNotificationWorker>()
                .setInputData(androidx.work.workDataOf(KEY_FORCE_EVENT_SYNC to forceEventSync))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME + "_now", androidx.work.ExistingWorkPolicy.REPLACE, request)
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ChatNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
