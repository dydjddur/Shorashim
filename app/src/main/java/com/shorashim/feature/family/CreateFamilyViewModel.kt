package com.shorashim.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.common.FamilyValidators
import com.shorashim.data.family.FamilyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateFamilyUiState(
    val name: String = "",
    val nameError: Int? = null,
    val isLoading: Boolean = false,
    val generalError: String? = null,
)

class CreateFamilyViewModel(
    private val familyRepository: FamilyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateFamilyUiState())
    val uiState: StateFlow<CreateFamilyUiState> = _uiState.asStateFlow()

    fun onNameChanged(newName: String) {
        _uiState.value = _uiState.value.copy(
            name = newName,
            nameError = null,
            generalError = null,
        )
    }

    fun createFamily(onSuccess: (String) -> Unit) {
        val current = _uiState.value
        if (current.isLoading) return

        val validationError = FamilyValidators.validateFamilyName(current.name)
        if (validationError != null) {
            _uiState.value = current.copy(nameError = validationError)
            return
        }

        _uiState.value = current.copy(isLoading = true, generalError = null)

        viewModelScope.launch {
            familyRepository.createFamily(current.name.trim())
                .onSuccess { family ->
                    _uiState.value = CreateFamilyUiState()
                    onSuccess(family.id)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalError = error.localizedMessage ?: "Failed to create family",
                    )
                }
        }
    }
}
