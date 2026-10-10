package com.shorashim.feature.tehillim

import androidx.lifecycle.ViewModel
import com.shorashim.data.psalms.PsalmChapter
import com.shorashim.data.psalms.PsalmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TehillimUiState(
    val allChapters: List<PsalmChapter> = emptyList(),
    val currentChapter: PsalmChapter? = null,
    val isMemorialMode: Boolean = false,
    val memorialChapters: List<PsalmChapter> = emptyList(),
    val memorialCurrentIndex: Int = 0,
    val deceasedName: String = "",
    val motherName: String = "",
    val isDrawerOpen: Boolean = false,
    val fontSizeSp: Int = 20,
) {
    val dedicationText: String
        get() {
            if (deceasedName.isBlank()) return "לעילוי נשמת כל נפטרי עמו ישראל"
            return if (motherName.isNotBlank()) {
                "לעילוי נשמת $deceasedName, $deceasedName בן/בת $motherName"
            } else {
                "לעילוי נשמת $deceasedName"
            }
        }
}

class TehillimViewModel(
    private val psalmsRepository: PsalmsRepository,
    initialDeceasedName: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TehillimUiState())
    val uiState: StateFlow<TehillimUiState> = _uiState.asStateFlow()

    init {
        val chapters = psalmsRepository.getAllChapters()
        val memorialChapters = psalmsRepository.getMemorialChapters()
        val firstChapter = chapters.firstOrNull()

        _uiState.update {
            it.copy(
                allChapters = chapters,
                currentChapter = firstChapter,
                memorialChapters = memorialChapters,
            )
        }

        if (!initialDeceasedName.isNullOrBlank()) {
            enterMemorialMode(initialDeceasedName)
        }
    }

    fun selectChapter(chapterNumber: Int) {
        val chapter = psalmsRepository.getChapter(chapterNumber) ?: return
        _uiState.update {
            it.copy(
                currentChapter = chapter,
                isMemorialMode = false,
                isDrawerOpen = false,
            )
        }
    }

    fun enterMemorialMode(deceasedName: String = "", motherName: String = "") {
        val memorialList = _uiState.value.memorialChapters
        val firstMemorialChapter = memorialList.firstOrNull() ?: _uiState.value.allChapters.firstOrNull()

        _uiState.update {
            it.copy(
                isMemorialMode = true,
                deceasedName = deceasedName,
                motherName = motherName,
                memorialCurrentIndex = 0,
                currentChapter = firstMemorialChapter,
                isDrawerOpen = false,
            )
        }
    }

    fun exitMemorialMode() {
        val currentNum = _uiState.value.currentChapter?.chapter ?: 1
        selectChapter(currentNum)
    }

    fun nextChapter() {
        val state = _uiState.value
        if (state.isMemorialMode) {
            val memorialList = state.memorialChapters
            if (memorialList.isEmpty()) return
            val nextIdx = (state.memorialCurrentIndex + 1).coerceAtMost(memorialList.size - 1)
            _uiState.update {
                it.copy(
                    memorialCurrentIndex = nextIdx,
                    currentChapter = memorialList[nextIdx],
                )
            }
        } else {
            val currentNum = state.currentChapter?.chapter ?: 1
            if (currentNum < 150) {
                selectChapter(currentNum + 1)
            }
        }
    }

    fun previousChapter() {
        val state = _uiState.value
        if (state.isMemorialMode) {
            val memorialList = state.memorialChapters
            if (memorialList.isEmpty()) return
            val prevIdx = (state.memorialCurrentIndex - 1).coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    memorialCurrentIndex = prevIdx,
                    currentChapter = memorialList[prevIdx],
                )
            }
        } else {
            val currentNum = state.currentChapter?.chapter ?: 1
            if (currentNum > 1) {
                selectChapter(currentNum - 1)
            }
        }
    }

    fun setDrawerOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isDrawerOpen = isOpen) }
    }

    fun updateDedicationNames(deceased: String, mother: String) {
        _uiState.update {
            it.copy(
                deceasedName = deceased,
                motherName = mother,
            )
        }
    }

    fun adjustFontSize(delta: Int) {
        _uiState.update {
            it.copy(fontSizeSp = (it.fontSizeSp + delta).coerceIn(14, 32))
        }
    }
}
