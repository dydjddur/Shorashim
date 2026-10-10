package com.shorashim.core.model

/**
 * Shared load states for data-driven screens.
 * Phase 1 repositories yield [Empty]; later phases emit the others from real data/errors.
 */
sealed interface ScreenContentState {
    data object Loading : ScreenContentState
    data object Empty : ScreenContentState
    data class Error(val message: String? = null) : ScreenContentState
    data object Ready : ScreenContentState
}
