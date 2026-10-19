package com.shorashim.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.model.CalendarEvent
import com.shorashim.core.model.Family
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.model.MemorialEventInsertDto
import com.shorashim.core.model.Person
import com.shorashim.core.notifications.EventNotificationScheduler
import com.shorashim.data.events.EventsRepository
import com.shorashim.data.family.FamilyRepository
import com.shorashim.data.family.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventsUiState(
    val isLoading: Boolean = true,
    val events: List<CalendarEvent> = emptyList(),
    val errorMessage: String? = null,
    val families: List<Family> = emptyList(),
    val deceasedPeople: List<Person> = emptyList(),
    val selectedEvent: CalendarEvent? = null,
    val isAddEditOpen: Boolean = false,
    val editingMemorial: CalendarEvent.Memorial? = null,
    val isSaving: Boolean = false,
    /** Failure of a save/delete action (shown inside the dialog), separate from a failed load. */
    val actionError: String? = null,
)

class EventsViewModel(
    private val eventsRepository: EventsRepository,
    private val familyRepository: FamilyRepository,
    private val personRepository: PersonRepository,
    private val notificationScheduler: EventNotificationScheduler? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        loadEvents(isSilent = false)
        loadFamiliesAndPeople()
        startSilentPolling()
    }

    private fun startSilentPolling() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000)
                loadEvents(isSilent = true)
                loadFamiliesAndPeople()
            }
        }
    }

    fun loadEvents(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent && _uiState.value.events.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            val result = eventsRepository.loadUpcomingEvents()
            result.onSuccess { events ->
                _uiState.update { it.copy(isLoading = false, events = events) }
                // Schedule local notifications for birthdays and memorials
                scheduleAllNotifications(events)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = if (isSilent && _uiState.value.events.isNotEmpty()) null else err.message) }
            }
        }
    }

    fun loadFamiliesAndPeople() {
        viewModelScope.launch {
            val fams = familyRepository.getMyFamilies().getOrElse { emptyList() }
            val allPeople = personRepository.getAllAccessiblePeople().getOrElse { emptyList() }
            val deceased = allPeople.filter { it.lifeStatus == LifeStatus.DECEASED }

            _uiState.update {
                it.copy(
                    families = fams,
                    deceasedPeople = deceased,
                )
            }
        }
    }

    private fun scheduleAllNotifications(events: List<CalendarEvent>) {
        if (notificationScheduler == null) return
        for (event in events) {
            when (event) {
                is CalendarEvent.Birthday -> {
                    notificationScheduler.scheduleBirthdayNotification(
                        personId = event.personId,
                        personName = event.personName,
                        targetDate = event.targetDate,
                    )
                }
                is CalendarEvent.Memorial -> {
                    notificationScheduler.scheduleMemorialReminder(
                        eventId = event.memorialId,
                        personName = event.personName,
                        targetDate = event.targetDate,
                    )
                }
            }
        }
    }

    fun onSelectEvent(event: CalendarEvent?) {
        _uiState.update { it.copy(selectedEvent = event) }
    }

    fun onOpenAddDialog() {
        _uiState.update {
            it.copy(
                actionError = null,
                isAddEditOpen = true,
                editingMemorial = null,
            )
        }
    }

    fun onOpenEditDialog(memorial: CalendarEvent.Memorial) {
        _uiState.update {
            it.copy(
                isAddEditOpen = true,
                editingMemorial = memorial,
                selectedEvent = null,
            )
        }
    }

    fun onCloseAddEditDialog() {
        _uiState.update {
            it.copy(
                isAddEditOpen = false,
                editingMemorial = null,
            )
        }
    }

    fun saveMemorialEvent(
        familyId: String,
        personId: String?,
        personName: String,
        eventDate: String,
        hebrewDate: String?,
        description: String?,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val insertDto = MemorialEventInsertDto(
                familyId = familyId,
                personId = personId,
                personName = personName.trim(),
                eventDate = eventDate.trim(),
                hebrewDate = hebrewDate?.trim()?.ifBlank { null },
                description = description?.trim()?.ifBlank { null },
            )

            val currentEdit = _uiState.value.editingMemorial
            val result = if (currentEdit != null) {
                eventsRepository.updateMemorialEvent(currentEdit.memorialId, insertDto)
            } else {
                eventsRepository.createMemorialEvent(insertDto)
            }

            result.onSuccess {
                _uiState.update { it.copy(isSaving = false, isAddEditOpen = false, editingMemorial = null) }
                loadEvents()
            }.onFailure { err ->
                _uiState.update { it.copy(isSaving = false, actionError = friendlyError(err)) }
            }
        }
    }

    fun deleteMemorialEvent(memorialId: String) {
        viewModelScope.launch {
            notificationScheduler?.cancelMemorialReminder(memorialId)
            eventsRepository.deleteMemorialEvent(memorialId).onSuccess {
                _uiState.update { it.copy(selectedEvent = null) }
                loadEvents()
            }.onFailure { err ->
                _uiState.update { it.copy(actionError = friendlyError(err)) }
            }
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    private fun friendlyError(t: Throwable): String {
        val raw = t.message.orEmpty()
        return if (raw.contains("memorial_events", ignoreCase = true) && raw.contains("schema cache", ignoreCase = true)) {
            "טבלת האזכרות עדיין לא קיימת בשרת (Supabase). יש להריץ את קובץ המיגרציה phase4_events_and_profiles.sql."
        } else {
            raw.ifBlank { "הפעולה נכשלה, נסו שוב" }
        }
    }
}
