package com.shorashim.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.R
import com.shorashim.core.common.FamilyValidators
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.model.PartialDate
import com.shorashim.core.model.Person
import com.shorashim.data.family.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddEditPersonUiState(
    val isEditing: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val additionalNames: String = "",
    val lifeStatus: LifeStatus = LifeStatus.LIVING,
    val birthYear: String = "",
    val birthMonth: String = "",
    val birthDay: String = "",
    val birthPrecision: DatePrecision = DatePrecision.FULL_DATE,
    val deathYear: String = "",
    val deathMonth: String = "",
    val deathDay: String = "",
    val deathPrecision: DatePrecision = DatePrecision.FULL_DATE,
    val biography: String = "",
    val isLoading: Boolean = false,
    val firstNameError: Int? = null,
    val dateError: Int? = null,
    val generalError: String? = null,
    val isDeleteConfirmOpen: Boolean = false,
)

class AddEditPersonViewModel(
    val familyId: String,
    val personId: String?,
    private val personRepository: PersonRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditPersonUiState(isEditing = personId != null))
    val uiState: StateFlow<AddEditPersonUiState> = _uiState.asStateFlow()

    init {
        if (personId != null) {
            loadPerson(personId)
        }
    }

    private fun loadPerson(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            personRepository.getPerson(id)
                .onSuccess { person ->
                    val bDate = PartialDate.fromIso(person.birthDate, person.birthDatePrecision)
                    val dDate = PartialDate.fromIso(person.deathDate, person.deathDatePrecision)

                    _uiState.value = _uiState.value.copy(
                        isEditing = true,
                        firstName = person.firstName,
                        lastName = person.lastName.orEmpty(),
                        additionalNames = person.additionalNames.orEmpty(),
                        lifeStatus = person.lifeStatus,
                        birthYear = bDate.year?.toString().orEmpty(),
                        birthMonth = bDate.month?.toString().orEmpty(),
                        birthDay = bDate.day?.toString().orEmpty(),
                        birthPrecision = bDate.precision,
                        deathYear = dDate.year?.toString().orEmpty(),
                        deathMonth = dDate.month?.toString().orEmpty(),
                        deathDay = dDate.day?.toString().orEmpty(),
                        deathPrecision = dDate.precision,
                        biography = person.biography.orEmpty(),
                        isLoading = false,
                    )
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalError = t.localizedMessage,
                    )
                }
        }
    }

    fun onFirstNameChanged(v: String) {
        _uiState.value = _uiState.value.copy(firstName = v, firstNameError = null)
    }

    fun onLastNameChanged(v: String) {
        _uiState.value = _uiState.value.copy(lastName = v)
    }

    fun onAdditionalNamesChanged(v: String) {
        _uiState.value = _uiState.value.copy(additionalNames = v)
    }

    fun onLifeStatusChanged(v: LifeStatus) {
        _uiState.value = _uiState.value.copy(lifeStatus = v, dateError = null)
    }

    fun onBirthYearChanged(v: String) {
        _uiState.value = _uiState.value.copy(birthYear = v, dateError = null)
    }

    fun onBirthMonthChanged(v: String) {
        _uiState.value = _uiState.value.copy(birthMonth = v, dateError = null)
    }

    fun onBirthDayChanged(v: String) {
        _uiState.value = _uiState.value.copy(birthDay = v, dateError = null)
    }

    fun onBirthPrecisionChanged(v: DatePrecision) {
        _uiState.value = _uiState.value.copy(birthPrecision = v, dateError = null)
    }

    fun onDeathYearChanged(v: String) {
        _uiState.value = _uiState.value.copy(deathYear = v, dateError = null)
    }

    fun onDeathMonthChanged(v: String) {
        _uiState.value = _uiState.value.copy(deathMonth = v, dateError = null)
    }

    fun onDeathDayChanged(v: String) {
        _uiState.value = _uiState.value.copy(deathDay = v, dateError = null)
    }

    fun onDeathPrecisionChanged(v: DatePrecision) {
        _uiState.value = _uiState.value.copy(deathPrecision = v, dateError = null)
    }

    fun onBiographyChanged(v: String) {
        _uiState.value = _uiState.value.copy(biography = v)
    }

    fun setDeleteConfirmOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isDeleteConfirmOpen = open)
    }

    fun save(onSuccess: () -> Unit) {
        val s = _uiState.value
        if (s.isLoading) return

        val nameErr = FamilyValidators.validateFirstName(s.firstName)
        if (nameErr != null) {
            _uiState.value = s.copy(firstNameError = nameErr)
            return
        }

        val bDate = buildPartialDate(s.birthYear, s.birthMonth, s.birthDay, s.birthPrecision)
        val dDate = if (s.lifeStatus == LifeStatus.DECEASED) {
            buildPartialDate(s.deathYear, s.deathMonth, s.deathDay, s.deathPrecision)
        } else null

        val dateErr = FamilyValidators.validatePersonDates(s.lifeStatus, bDate, dDate)
        if (dateErr != null) {
            _uiState.value = s.copy(dateError = dateErr)
            return
        }

        _uiState.value = s.copy(isLoading = true, generalError = null)

        viewModelScope.launch {
            val person = Person(
                id = personId ?: "",
                familyId = familyId,
                firstName = s.firstName.trim(),
                lastName = s.lastName.trim().ifBlank { null },
                additionalNames = s.additionalNames.trim().ifBlank { null },
                lifeStatus = s.lifeStatus,
                birthDate = bDate?.toIsoDateString(),
                birthDatePrecision = bDate?.precision,
                deathDate = dDate?.toIsoDateString(),
                deathDatePrecision = dDate?.precision,
                biography = s.biography.trim().ifBlank { null },
            )

            val result = if (personId == null) {
                personRepository.createPerson(person)
            } else {
                personRepository.updatePerson(person)
            }

            result
                .onSuccess { onSuccess() }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalError = t.localizedMessage ?: "Failed to save person",
                    )
                }
        }
    }

    fun delete(onSuccess: () -> Unit) {
        val id = personId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            personRepository.deletePerson(id)
                .onSuccess { onSuccess() }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalError = t.localizedMessage ?: "Failed to delete person",
                    )
                }
        }
    }

    private fun buildPartialDate(yStr: String, mStr: String, dStr: String, prec: DatePrecision): PartialDate? {
        if (prec == DatePrecision.UNKNOWN) return PartialDate(precision = DatePrecision.UNKNOWN)
        val y = yStr.toIntOrNull() ?: return null
        val m = mStr.toIntOrNull()
        val d = dStr.toIntOrNull()
        return PartialDate(year = y, month = m, day = d, precision = prec)
    }
}
