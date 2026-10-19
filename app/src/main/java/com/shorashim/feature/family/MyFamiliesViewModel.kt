package com.shorashim.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.Family
import com.shorashim.data.family.FamilyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MyFamiliesUiState {
    data object Loading : MyFamiliesUiState
    data object Empty : MyFamiliesUiState
    data class Success(val families: List<Family>) : MyFamiliesUiState
    data class Error(val message: String? = null) : MyFamiliesUiState
}

class MyFamiliesViewModel(
    private val familyRepository: FamilyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyFamiliesUiState>(MyFamiliesUiState.Loading)
    val uiState: StateFlow<MyFamiliesUiState> = _uiState.asStateFlow()

    init {
        loadFamilies(isSilent = false)
        startSilentPolling()
    }

    private fun startSilentPolling() {
        viewModelScope.launch {
            com.shorashim.core.common.pollWhileForeground(20_000) {
                loadFamilies(isSilent = true)
            }
        }
    }

    fun loadFamilies(isSilent: Boolean = false) {
        viewModelScope.launch {
            val isAlreadyLoaded = _uiState.value is MyFamiliesUiState.Success || _uiState.value is MyFamiliesUiState.Empty
            if (!isSilent && !isAlreadyLoaded) {
                _uiState.value = MyFamiliesUiState.Loading
            }
            familyRepository.getMyFamilies()
                .onSuccess { list ->
                    if (list.isEmpty()) {
                        _uiState.value = MyFamiliesUiState.Empty
                    } else {
                        _uiState.value = MyFamiliesUiState.Success(list)
                    }
                }
                .onFailure { error ->
                    // If we already have loaded content and it's a silent refresh, don't destroy the screen
                    if (isAlreadyLoaded) {
                        return@onFailure
                    }
                    val rawMsg = error.localizedMessage.orEmpty()
                    val friendlyMsg = when {
                        rawMsg.contains("Could not find the table", ignoreCase = true) ->
                            "טבלאות מסד הנתונים טרם נוצרו ב-Supabase. אנא הריצו את סקריפט המיגרציה בלוח הבקרה של Supabase (קובץ supabase/migrations/20261011_phase3_schema.sql)."
                        rawMsg.contains("Failed to connect", ignoreCase = true) || rawMsg.contains("Unable to resolve host", ignoreCase = true) ->
                            "שגיאת תקשורת עם השרת. אנא בדקו את החיבור לאינטרנט."
                        else -> rawMsg.ifBlank { "אירעה שגיאה בטעינת עצי המשפחה" }
                    }
                    _uiState.value = MyFamiliesUiState.Error(friendlyMsg)
                }
        }
    }
}
