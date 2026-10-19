package com.shorashim.core.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Tracks whether the app UI is in the foreground, so background polling loops in ViewModels
 * pause while the app is minimized instead of draining battery and data.
 *
 * Starts as `false` until MainActivity.onStart reports the UI as visible. Besides saving battery, this keeps unit
 * tests (which never start an Activity) from hanging: an endless `delay` loop never lets `advanceUntilIdle()` finish,
 * whereas a loop parked in [awaitForeground] leaves the test scheduler idle.
 */
object AppVisibility {
    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    fun setForeground(foreground: Boolean) {
        _isForeground.value = foreground
    }

    suspend fun awaitForeground() {
        if (!_isForeground.value) _isForeground.first { it }
    }
}

/**
 * Runs [block] every [intervalMs] while the coroutine is active, skipping ticks while the app is backgrounded.
 */
suspend fun pollWhileForeground(intervalMs: Long, block: suspend () -> Unit) {
    while (coroutineContext.isActive) {
        delay(intervalMs)
        AppVisibility.awaitForeground()
        block()
    }
}
