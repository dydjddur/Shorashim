package com.shorashim.feature.memorials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.MemorialSummary
import com.shorashim.core.model.ScreenContentState
import com.shorashim.data.memorials.MemorialRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MemorialsUiState(
    val content: ScreenContentState = ScreenContentState.Loading,
    val summary: MemorialSummary = MemorialSummary(),
)

class MemorialsViewModel(
    memorialRepository: MemorialRepository,
) : ViewModel() {
    val uiState: StateFlow<MemorialsUiState> = memorialRepository.observeMemorialSummary()
        .map { summary ->
            val content = if (summary.upcomingCount == 0) {
                ScreenContentState.Empty
            } else {
                ScreenContentState.Ready
            }
            MemorialsUiState(content = content, summary = summary)
        }
        .catch { emit(MemorialsUiState(content = ScreenContentState.Error())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MemorialsUiState(),
        )
}
