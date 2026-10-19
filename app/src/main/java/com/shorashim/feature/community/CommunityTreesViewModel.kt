package com.shorashim.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.PublicFamilyTree
import com.shorashim.data.family.FamilyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommunityTreesUiState(
    val trees: List<PublicFamilyTree> = emptyList(),
    val filteredTrees: List<PublicFamilyTree> = emptyList(),
    val mySurnames: List<String> = emptyList(),
    val popularSurnames: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedSurnameFilter: String? = null,
    val onlyMySurnamesMatch: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

class CommunityTreesViewModel(
    private val familyRepository: FamilyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityTreesUiState(isLoading = true))
    val uiState: StateFlow<CommunityTreesUiState> = _uiState.asStateFlow()

    init {
        loadData(isSilent = false)
        startSilentPolling()
    }

    private fun startSilentPolling() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(25_000)
                loadData(isSilent = true)
            }
        }
    }

    fun loadData(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent && _uiState.value.trees.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            } else if (!isSilent) {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            }

            val treesResult = familyRepository.getPublicTrees()
            val mySurnamesResult = familyRepository.getMySurnames()

            if (treesResult.isSuccess) {
                val trees = treesResult.getOrDefault(emptyList())
                val mySurnames = mySurnamesResult.getOrDefault(emptyList())

                // Extract all distinct surnames from all trees to create popular surnames list
                val popularSurnames = trees
                    .flatMap { it.surnames }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .map { it.key }
                    .take(15)

                val filtered = applyFilters(
                    trees = trees,
                    query = _uiState.value.searchQuery,
                    selectedSurname = _uiState.value.selectedSurnameFilter,
                    onlyMySurnames = _uiState.value.onlyMySurnamesMatch,
                    mySurnames = mySurnames,
                )

                _uiState.value = _uiState.value.copy(
                    trees = trees,
                    filteredTrees = filtered,
                    mySurnames = mySurnames,
                    popularSurnames = popularSurnames,
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                )
            } else {
                val msg = treesResult.exceptionOrNull()?.message ?: "שגיאה בטעינת עצי המשפחה הקהילתיים"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = if (isSilent && _uiState.value.trees.isNotEmpty()) null else msg,
                )
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        val filtered = applyFilters(
            trees = _uiState.value.trees,
            query = newQuery,
            selectedSurname = _uiState.value.selectedSurnameFilter,
            onlyMySurnames = _uiState.value.onlyMySurnamesMatch,
            mySurnames = _uiState.value.mySurnames,
        )
        _uiState.value = _uiState.value.copy(
            searchQuery = newQuery,
            filteredTrees = filtered,
        )
    }

    fun onSelectSurnameFilter(surname: String?) {
        val newSelection = if (_uiState.value.selectedSurnameFilter == surname) null else surname
        val filtered = applyFilters(
            trees = _uiState.value.trees,
            query = _uiState.value.searchQuery,
            selectedSurname = newSelection,
            onlyMySurnames = _uiState.value.onlyMySurnamesMatch,
            mySurnames = _uiState.value.mySurnames,
        )
        _uiState.value = _uiState.value.copy(
            selectedSurnameFilter = newSelection,
            filteredTrees = filtered,
        )
    }

    fun toggleOnlyMySurnamesMatch() {
        val newToggle = !_uiState.value.onlyMySurnamesMatch
        val filtered = applyFilters(
            trees = _uiState.value.trees,
            query = _uiState.value.searchQuery,
            selectedSurname = _uiState.value.selectedSurnameFilter,
            onlyMySurnames = newToggle,
            mySurnames = _uiState.value.mySurnames,
        )
        _uiState.value = _uiState.value.copy(
            onlyMySurnamesMatch = newToggle,
            filteredTrees = filtered,
        )
    }

    fun clearFilters() {
        val filtered = applyFilters(
            trees = _uiState.value.trees,
            query = "",
            selectedSurname = null,
            onlyMySurnames = false,
            mySurnames = _uiState.value.mySurnames,
        )
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            selectedSurnameFilter = null,
            onlyMySurnamesMatch = false,
            filteredTrees = filtered,
        )
    }

    private fun applyFilters(
        trees: List<PublicFamilyTree>,
        query: String,
        selectedSurname: String?,
        onlyMySurnames: Boolean,
        mySurnames: List<String>,
    ): List<PublicFamilyTree> {
        val q = query.trim()
        val mySurnamesSet = mySurnames.map { it.trim().lowercase() }.toSet()

        return trees.filter { tree ->
            // 1. Text search match
            val matchesQuery = if (q.isBlank()) true else {
                tree.name.contains(q, ignoreCase = true) ||
                    tree.description?.contains(q, ignoreCase = true) == true ||
                    tree.ownerName?.contains(q, ignoreCase = true) == true ||
                    tree.surnames.any { it.contains(q, ignoreCase = true) }
            }

            // 2. Specific surname chip match
            val matchesSelectedSurname = if (selectedSurname.isNullOrBlank()) true else {
                tree.surnames.any { it.equals(selectedSurname, ignoreCase = true) }
            }

            // 3. User's own surnames intersection match
            val matchesMySurnames = if (!onlyMySurnames) true else {
                tree.surnames.any { it.trim().lowercase() in mySurnamesSet }
            }

            matchesQuery && matchesSelectedSurname && matchesMySurnames
        }
    }
}
