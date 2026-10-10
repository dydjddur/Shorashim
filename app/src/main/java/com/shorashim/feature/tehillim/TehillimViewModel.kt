package com.shorashim.feature.tehillim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.ScreenContentState
import com.shorashim.core.model.TehillimCatalogStatus
import com.shorashim.data.psalms.PsalmsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TehillimUiState(
    val content: ScreenContentState = ScreenContentState.Loading,
    val catalog: TehillimCatalogStatus = TehillimCatalogStatus(),
)

class TehillimViewModel(
    psalmsRepository: PsalmsRepository,
) : ViewModel() {
    val uiState: StateFlow<TehillimUiState> = psalmsRepository.observeCatalogStatus()
        .map { catalog ->
            val content = if (!catalog.verifiedTextAvailable) {
                ScreenContentState.Empty
            } else {
                ScreenContentState.Ready
            }
            TehillimUiState(content = content, catalog = catalog)
        }
        .catch { emit(TehillimUiState(content = ScreenContentState.Error())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TehillimUiState(),
        )
}
