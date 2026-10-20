package com.shorashim

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shorashim.core.model.DecryptedChatMessage
import com.shorashim.core.model.EventDateCalculator
import com.shorashim.core.model.MessageType
import com.shorashim.core.notifications.ChatMessageNotifier
import com.shorashim.core.notifications.EventNotificationScheduler
import com.shorashim.data.chat.ChatRepository
import com.shorashim.data.chat.IncomingChatMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Runs on a device/emulator and checks that reminders and chat notifications really reach the system notification
 * shade (NotificationManager.activeNotifications), including a real AlarmManager alarm firing.
 */
@RunWith(AndroidJUnit4::class)
class NotificationsDeviceTest {
    private val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val nm = ctx.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(ctx.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        nm.cancelAll()
    }

    private fun titles(): List<String> =
        nm.activeNotifications.mapNotNull { it.notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() }

    private fun texts(): List<String> =
        nm.activeNotifications.mapNotNull { it.notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString() }

    private fun israelToday(hour: Int): ZonedDateTime =
        ZonedDateTime.of(EventDateCalculator.getTodayInIsrael(), LocalTime.of(hour, 0), EventDateCalculator.ISRAEL_ZONE)

    @Test
    fun birthdayToday_afterNine_isShownImmediately_andOnlyOnce() {
        val scheduler = EventNotificationScheduler(ctx, now = { israelToday(12) })
        val id = "test-person-" + UUID.randomUUID()

        scheduler.scheduleBirthdayNotification(id, "דני בדיקה", EventDateCalculator.getTodayInIsrael())
        val first = titles().count { it.contains("יום הולדת") }
        assertEquals("birthday notification should be posted", 1, first)
        assertTrue(texts().any { it.contains("דני בדיקה") })

        // Re-scheduling (every refresh does this) must not post it again.
        nm.cancelAll()
        scheduler.scheduleBirthdayNotification(id, "דני בדיקה", EventDateCalculator.getTodayInIsrael())
        assertEquals(0, titles().count { it.contains("יום הולדת") })
    }

    @Test
    fun memorial_weekBefore_andDayOf_areShown() {
        val scheduler = EventNotificationScheduler(ctx, now = { israelToday(12) })
        val today = EventDateCalculator.getTodayInIsrael()

        scheduler.scheduleMemorialReminder("test-mem-" + UUID.randomUUID(), "סבא בדיקה", today.plusDays(7))
        assertTrue("week-before reminder", titles().any { it.contains("בעוד שבוע") && it.contains("סבא בדיקה") })

        scheduler.scheduleMemorialReminder("test-mem-" + UUID.randomUUID(), "סבתא בדיקה", today)
        assertTrue("day-of reminder", titles().any { it.contains("היום האזכרה") && it.contains("סבתא בדיקה") })
    }

    @Test
    fun realAlarm_firesAndPostsNotification() {
        val scheduler = EventNotificationScheduler(ctx)
        val title = "בדיקת התראה " + UUID.randomUUID().toString().take(6)
        scheduler.scheduleAlarm(
            requestCode = 424242,
            triggerTimeEpochMs = System.currentTimeMillis() + 3_000,
            title = title,
            message = "האזעקה הופעלה",
            eventId = "alarm-test",
            eventType = EventNotificationScheduler.TYPE_BIRTHDAY,
            dedupeKey = "alarm-test|" + UUID.randomUUID(),
        )
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline && title !in titles()) Thread.sleep(500)
        assertTrue("alarm should fire and post '$title'", title in titles())
    }

    @Test
    fun newChatMessage_postsNotificationWithSenderAndText() = runBlocking {
        val conversationId = "conv-" + UUID.randomUUID()
        val repo = object : ChatRepository {
            override suspend fun ensurePublicKeyPublished() = Result.success(Unit)
            override suspend fun getPeerPublicKey(userId: String) = Result.success<String?>(null)
            override suspend fun getConversations() = Result.success(emptyList<com.shorashim.core.model.Conversation>())
            override suspend fun getOrCreateConversation(otherUserId: String) = Result.success(conversationId)
            override suspend fun getMessages(conversationId: String) = Result.success(emptyList<DecryptedChatMessage>())
            override suspend fun sendTextMessage(conversationId: String, recipientUserId: String, text: String) =
                Result.failure<DecryptedChatMessage>(UnsupportedOperationException())
            override suspend fun sendInvitationMessage(
                conversationId: String, recipientUserId: String, invitationUrl: String, familyName: String?, token: String?,
            ) = Result.failure<DecryptedChatMessage>(UnsupportedOperationException())
            override suspend fun markMessagesAsRead(conversationId: String) = Result.success(Unit)
            override suspend fun getIncomingUnreadSince(sinceIso: String?) = Result.success(
                listOf(
                    IncomingChatMessage(
                        message = DecryptedChatMessage(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            senderId = "friend-id",
                            recipientId = "me",
                            messageType = MessageType.TEXT,
                            plaintext = "מה נשמע? נדבר מחר",
                            isRead = false,
                            createdAt = java.time.Instant.now().toString(),
                            isFromMe = false,
                        ),
                        senderName = "נאור",
                        previewText = "מה נשמע? נדבר מחר",
                    ),
                ),
            )
        }
        // Simulate an existing install (first run only records "now" and stays quiet).
        ctx.getSharedPreferences("shorashim_chat_notifications", Context.MODE_PRIVATE)
            .edit().putString("last_seen_created_at", "2020-01-01T00:00:00Z").commit()

        ChatMessageNotifier.checkAndNotify(ctx, repo)

        assertTrue("sender name as title", "נאור" in titles())
        assertTrue("message text shown", texts().any { it.contains("מה נשמע? נדבר מחר") })
    }
}
