package com.shorashim.core.update

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppUpdateManager {
    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    var isDismissedForSession: Boolean = false
        private set

    private var checkJob: Job? = null

    fun start(scope: CoroutineScope, checkIntervalMs: Long = 10 * 60 * 1000L) {
        checkJob?.cancel()
        checkJob = scope.launch {
            // 1. Check immediately on launch
            checkNow()

            // 2. Periodic check every 10 minutes
            while (isActive) {
                delay(checkIntervalMs)
                if (!isDismissedForSession && _updateInfo.value == null) {
                    checkNow()
                }
            }
        }
    }

    suspend fun checkNow() {
        try {
            val info = AppUpdateChecker.checkForUpdate()
            if (info != null && !isDismissedForSession) {
                _updateInfo.value = info
            }
        } catch (e: Exception) {
            Log.w("AppUpdateManager", "Failed to check for update", e)
        }
    }

    fun dismiss(dontAskAgainForSession: Boolean) {
        if (dontAskAgainForSession) {
            isDismissedForSession = true
        }
        _updateInfo.value = null
    }

    fun resetSessionDismissal() {
        isDismissedForSession = false
    }
}
