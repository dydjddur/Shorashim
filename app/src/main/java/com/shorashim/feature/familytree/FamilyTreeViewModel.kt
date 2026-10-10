package com.shorashim.feature.familytree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.FamilySummary
import com.shorashim.core.model.ScreenContentState
import com.shorashim.data.family.FamilyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FamilyTreeUiState(
    val content: ScreenContentState = ScreenContentState.Loading,
    val summary: FamilySummary = FamilySummary(),
)

class FamilyTreeViewModel(
    familyRepository: FamilyRepository,
) : ViewModel() {
    val uiState: StateFlow<FamilyTreeUiState> = familyRepository.observeFamilySummary()
        .map { summary ->
            val content = if (summary.familyId == null) {
                ScreenContentState.Empty
            } else {
                ScreenContentState.Ready
            }
            FamilyTreeUiState(content = content, summary = summary)
        }
        .catch { emit(FamilyTreeUiState(content = ScreenContentState.Error())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FamilyTreeUiState(),
        )
}
