package com.shorashim

import com.shorashim.core.update.AppUpdateChecker
import com.shorashim.core.update.AppUpdateInfo
import com.shorashim.core.update.AppUpdateManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun testIsNewerVersion() {
        // Newer versions
        assertTrue(AppUpdateChecker.isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.0.0", "v1.0.1"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.0.0", "1.1.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.0.0", "2.0.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.0", "1.0.1"))
        assertTrue(AppUpdateChecker.isNewerVersion("v1.0.0", "v1.0.1"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.0.0", "1.0.0.1"))

        // Same version
        assertFalse(AppUpdateChecker.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.0.0", "v1.0.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("v1.0.0", "1.0.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("v1.0.0", "v1.0.0"))

        // Older versions
        assertFalse(AppUpdateChecker.isNewerVersion("1.0.1", "1.0.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("2.0.0", "1.9.9"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.1.0", "1.0.5"))
    }

    @Test
    fun testAppUpdateManagerSessionDismissal() {
        val manager = AppUpdateManager()
        assertFalse(manager.isDismissedForSession)
        assertNull(manager.updateInfo.value)

        // Dismiss without "don't ask again"
        manager.dismiss(dontAskAgainForSession = false)
        assertFalse(manager.isDismissedForSession)

        // Dismiss with "don't ask again"
        manager.dismiss(dontAskAgainForSession = true)
        assertTrue(manager.isDismissedForSession)

        // Reset
        manager.resetSessionDismissal()
        assertFalse(manager.isDismissedForSession)
    }
}
