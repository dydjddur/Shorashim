package com.shorashim.core.notifications

import android.Manifest
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
import com.shorashim.core.common.AppVisibility
import com.shorashim.data.chat.ChatRepository
import com.shorashim.data.chat.IncomingChatMessage
import com.shorashim.data.repositories.AppContainer
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Posts a phone notification when a friend sends a chat message.
 *
 * The app has no push server (FCM), so new messages are discovered by asking Supabase for unread messages addressed
 * to the user that arrived after the last one we notified about:
 * - every [IN_APP_INTERVAL_MS] while the app process is alive (foreground or recently backgrounded), and
 * - from [ChatNotificationWorker] roughly every 15 minutes after Android has closed the app.
 * Messages are decrypted on the device, so the notification can show the actual text.
 */
object ChatMessageNotifier {
    const val CHANNEL_ID = "shorashim_chat_messages"
    const val EXTRA_OPEN_CHAT_USER_ID = "open_chat_user_id"
    const val EXTRA_OPEN_CHAT_USER_NAME = "open_chat_user_name"

    private const val TAG = "ChatNotifier"
    private const val PREFS = "shorashim_chat_notifications"
    private const val KEY_LAST_SEEN = "last_seen_created_at"
    private const val IN_APP_INTERVAL_MS = 20_000L

    /** Conversation currently open on screen; its messages are not notified while the app is visible. */
    @Volatile
    var activeConversationId: String? = null

    private val mutex = Mutex()

    fun startInAppChecks(context: Context, container: AppContainer) {
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            while (true) {
                delay(IN_APP_INTERVAL_MS)
                val client = container.supabaseClient ?: continue
                if (client.auth.currentUserOrNull() == null) continue
                runCatching { checkAndNotify(appContext, container.chatRepository) }
                    .onFailure { Log.w(TAG, "In-app message check failed: ${it.message}") }
            }
        }
    }

    /** Fetches new unread messages and posts one notification per conversation. */
    suspend fun checkAndNotify(context: Context, chatRepository: ChatRepository) = mutex.withLock {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastSeen = prefs.getString(KEY_LAST_SEEN, null)
        if (lastSeen == null) {
            // First run on this device: start from "now" so a backlog of old unread messages doesn't flood the user.
            prefs.edit().putString(KEY_LAST_SEEN, java.time.Instant.now().toString()).apply()
            return@withLock
        }
        // Normalise to "...Z" so the value is safe inside the PostgREST query string (a "+00:00" offset is not).
        val since = com.shorashim.core.common.TimeFormat.parseInstant(lastSeen)?.toString() ?: lastSeen

        val incoming = chatRepository.getIncomingUnreadSince(since).getOrElse {
            Log.w(TAG, "Could not fetch new messages: ${it.message}")
            return@withLock
        }
        if (incoming.isEmpty()) return@withLock

        val newest = incoming.mapNotNull { it.message.createdAt }
            .maxByOrNull { com.shorashim.core.common.TimeFormat.parseInstant(it) ?: java.time.Instant.EPOCH }
        if (newest != null) prefs.edit().putString(KEY_LAST_SEEN, newest).apply()

        ensureChannel(context)
        incoming
            .filterNot { AppVisibility.isForeground.value && it.message.conversationId == activeConversationId }
            .groupBy { it.message.conversationId }
            .forEach { (conversationId, messages) -> show(context, conversationId, messages) }
    }

    fun cancel(context: Context, conversationId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(conversationId))
    }

    private fun show(context: Context, conversationId: String, messages: List<IncomingChatMessage>) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val last = messages.last()
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CHAT_USER_ID, last.message.senderId)
            putExtra(EXTRA_OPEN_CHAT_USER_NAME, last.senderName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId(conversationId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val style = NotificationCompat.InboxStyle().setBigContentTitle(last.senderName)
        messages.takeLast(6).forEach { style.addLine(it.previewText) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(last.senderName)
            .setContentText(
                if (messages.size > 1) "${messages.size} הודעות חדשות · ${last.previewText}" else last.previewText,
            )
            .setStyle(if (messages.size > 1) style else NotificationCompat.BigTextStyle().bigText(last.previewText))
            .setNumber(messages.size)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(notificationId(conversationId), notification) }
            .onFailure { Log.w(TAG, "Could not post notification: ${it.message}") }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "הודעות צ'אט", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "התראה כשחבר שולח לך הודעה"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun notificationId(conversationId: String): Int = 0x5000_0000 or (conversationId.hashCode() and 0x0FFF_FFFF)
}
