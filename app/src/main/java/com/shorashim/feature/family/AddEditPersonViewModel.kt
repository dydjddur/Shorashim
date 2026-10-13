package com.shorashim.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.R
import com.shorashim.core.common.FamilyValidators
import com.shorashim.core.common.LastNameDefaults
import com.shorashim.data.family.FamilyRepository
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.model.NewPersonRelation
import com.shorashim.core.model.RelationshipType
import com.shorashim.data.family.RelationshipRepository
import com.shorashim.core.model.PartialDate
import com.shorashim.core.model.Person
import com.shorashim.data.family.PersonRepository
import com.shorashim.core.common.HebrewDates
import com.shorashim.core.model.HebrewCalendarMath
import com.shorashim.core.model.HebrewDate
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CalendarInputType {
    GREGORIAN,
    HEBREW,
}

data class AddEditPersonUiState(
    val isEditing: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val additionalNames: String = "",
    val photoPath: String? = null,
    val lifeStatus: LifeStatus = LifeStatus.LIVING,
    val birthYear: String = "",
    val birthMonth: String = "",
    val birthDay: String = "",
    val birthPrecision: DatePrecision = DatePrecision.FULL_DATE,
    val birthCalendarType: CalendarInputType = CalendarInputType.GREGORIAN,
    val birthHebrewYear: String = "",
    val birthHebrewMonth: Int = HebrewCalendarMath.TISHREI,
    val birthHebrewDay: String = "",
    val birthHebrewFormatted: String = "",
    val deathYear: String = "",
    val deathMonth: String = "",
    val deathDay: String = "",
    val deathPrecision: DatePrecision = DatePrecision.FULL_DATE,
    val deathCalendarType: CalendarInputType = CalendarInputType.GREGORIAN,
    val deathHebrewYear: String = "",
    val deathHebrewMonth: Int = HebrewCalendarMath.TISHREI,
    val deathHebrewDay: String = "",
    val deathHebrewFormatted: String = "",
    val biography: String = "",
    val isLoading: Boolean = false,
    val firstNameError: Int? = null,
    val dateError: Int? = null,
    val generalError: String? = null,
    val isDeleteConfirmOpen: Boolean = false,
    // --- Adding a relative from a tree card ---
    val relation: NewPersonRelation? = null,
    val anchorName: String = "",
    /** Spouses of the anchor that may be the other parent of a new child. */
    val coParentOptions: List<Person> = emptyList(),
    val selectedCoParentId: String? = null,
    /** When adding a parent to someone who already has exactly one parent: that parent. */
    val existingParent: Person? = null,
    val linkToExistingParent: Boolean = true,
    /** A sibling needs the anchor's parents; without them the relation cannot be stored. */
    val siblingBlocked: Boolean = false,
)

class AddEditPersonViewModel(
    val familyId: String,
    val personId: String?,
    private val personRepository: PersonRepository,
    private val relationshipRepository: RelationshipRepository? = null,
    private val relation: NewPersonRelation? = null,
    private val anchorId: String? = null,
    private val familyRepository: FamilyRepository? = null,
) : ViewModel() {

    private var anchorParentIds: List<String> = emptyList()

    // Context for the suggested family name.
    private var treeName: String? = null
    private var anchorPerson: Person? = null
    private var anchorHasParents = false
    private var peopleWithParents: Set<String> = emptySet()

    /** Once the user typed a family name themselves we stop suggesting one. */
    private var lastNameEditedByUser = false

    private val _uiState = MutableStateFlow(AddEditPersonUiState(isEditing = personId != null))
    val uiState: StateFlow<AddEditPersonUiState> = _uiState.asStateFlow()

    init {
        if (personId != null) {
            loadPerson(personId)
        } else if (relation != null && anchorId != null) {
            loadAnchorContext(relation, anchorId)
        } else {
            loadTreeNameDefault()
        }
    }

    /** A loose person (no relative picked) defaults to the tree's family name. */
    private fun loadTreeNameDefault() {
        viewModelScope.launch {
            treeName = familyRepository?.getFamily(familyId)?.getOrNull()?.name
            applySuggestedLastName()
        }
    }

    private fun applySuggestedLastName() {
        if (lastNameEditedByUser || personId != null) return
        val s = _uiState.value
        val coParent = s.coParentOptions.firstOrNull { it.id == s.selectedCoParentId }
        val suggestion = LastNameDefaults.suggest(
            relation = relation,
            treeName = treeName,
            anchor = anchorPerson,
            anchorHasParents = anchorHasParents,
            coParent = coParent,
            coParentHasParents = coParent?.id in peopleWithParents,
            existingParent = s.existingParent,
        )
        _uiState.value = s.copy(lastName = suggestion)
    }

    private fun loadAnchorContext(relation: NewPersonRelation, anchorId: String) {
        _uiState.value = _uiState.value.copy(relation = relation)
        viewModelScope.launch {
            val people = personRepository.getPeople(familyId).getOrElse { emptyList() }
            val rels = relationshipRepository?.getRelationships(familyId)?.getOrElse { emptyList() }.orEmpty()
            val byId = people.associateBy { it.id }
            val anchor = byId[anchorId] ?: return@launch
            treeName = familyRepository?.getFamily(familyId)?.getOrNull()?.name
            anchorPerson = anchor
            peopleWithParents = rels
                .filter { it.relationshipType == RelationshipType.PARENT_CHILD }
                .map { it.relatedPersonId }
                .toSet()
            anchorHasParents = anchorId in peopleWithParents

            val parents = rels
                .filter { it.relationshipType == RelationshipType.PARENT_CHILD && it.relatedPersonId == anchorId }
                .mapNotNull { byId[it.personId] }
            val spouses = rels
                .filter { it.relationshipType == RelationshipType.SPOUSE && (it.personId == anchorId || it.relatedPersonId == anchorId) }
                .mapNotNull { byId[if (it.personId == anchorId) it.relatedPersonId else it.personId] }
                .distinctBy { it.id }
            anchorParentIds = parents.map { it.id }

            _uiState.value = _uiState.value.copy(
                anchorName = anchor.fullName,
                coParentOptions = if (relation == NewPersonRelation.CHILD) spouses else emptyList(),
                selectedCoParentId = if (relation == NewPersonRelation.CHILD) spouses.singleOrNull()?.id else null,
                existingParent = if (relation == NewPersonRelation.PARENT) parents.singleOrNull() else null,
                siblingBlocked = relation == NewPersonRelation.SIBLING && parents.isEmpty(),
            )
            applySuggestedLastName()
        }
    }

    fun onCoParentSelected(id: String?) {
        _uiState.value = _uiState.value.copy(selectedCoParentId = id)
        applySuggestedLastName()
    }

    fun onLinkToExistingParentChanged(link: Boolean) {
        _uiState.value = _uiState.value.copy(linkToExistingParent = link)
    }

    /** Stores the relationship(s) between a freshly created [created] person and the anchor. */
    private suspend fun createRelationships(created: Person): Result<Unit> {
        val repo = relationshipRepository
        val anchor = anchorId
        val rel = relation
        if (repo == null || anchor == null || rel == null) return Result.success(Unit)
        val s = _uiState.value
        return runCatching {
            when (rel) {
                NewPersonRelation.PARENT -> {
                    repo.createRelationship(familyId, created.id, anchor, RelationshipType.PARENT_CHILD).getOrThrow()
                    val other = s.existingParent
                    if (other != null && s.linkToExistingParent) {
                        repo.createRelationship(familyId, other.id, created.id, RelationshipType.SPOUSE).getOrThrow()
                    }
                }
                NewPersonRelation.CHILD -> {
                    repo.createRelationship(familyId, anchor, created.id, RelationshipType.PARENT_CHILD).getOrThrow()
                    s.selectedCoParentId?.let {
                        repo.createRelationship(familyId, it, created.id, RelationshipType.PARENT_CHILD).getOrThrow()
                    }
                }
                NewPersonRelation.SPOUSE ->
                    repo.createRelationship(familyId, anchor, created.id, RelationshipType.SPOUSE).getOrThrow()
                NewPersonRelation.SIBLING -> {
                    check(anchorParentIds.isNotEmpty()) { "no parents" }
                    anchorParentIds.forEach {
                        repo.createRelationship(familyId, it, created.id, RelationshipType.PARENT_CHILD).getOrThrow()
                    }
                }
            }
        }
    }

    private fun loadPerson(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            personRepository.getPerson(id)
                .onSuccess { person ->
                    val bDate = PartialDate.fromIso(person.birthDate, person.birthDatePrecision)
                    val dDate = PartialDate.fromIso(person.deathDate, person.deathDatePrecision)

                    val bY = bDate.year?.toString().orEmpty()
                    val bM = bDate.month?.toString().orEmpty()
                    val bD = bDate.day?.toString().orEmpty()
                    val (bHDate, bFormatted) = syncHebrewFromGregorian(bY, bM, bD, bDate.precision)

                    val dY = dDate.year?.toString().orEmpty()
                    val dM = dDate.month?.toString().orEmpty()
                    val dD = dDate.day?.toString().orEmpty()
                    val (dHDate, dFormatted) = syncHebrewFromGregorian(dY, dM, dD, dDate.precision)

                    _uiState.value = _uiState.value.copy(
                        isEditing = true,
                        firstName = person.firstName,
                        lastName = person.lastName.orEmpty(),
                        additionalNames = person.additionalNames.orEmpty(),
                        photoPath = person.photoPath,
                        lifeStatus = person.lifeStatus,
                        birthYear = bY,
                        birthMonth = bM,
                        birthDay = bD,
                        birthPrecision = bDate.precision,
                        birthHebrewYear = bHDate?.year?.toString().orEmpty(),
                        birthHebrewMonth = bHDate?.month ?: HebrewCalendarMath.TISHREI,
                        birthHebrewDay = bHDate?.day?.toString().orEmpty(),
                        birthHebrewFormatted = bFormatted,
                        deathYear = dY,
                        deathMonth = dM,
                        deathDay = dD,
                        deathPrecision = dDate.precision,
                        deathHebrewYear = dHDate?.year?.toString().orEmpty(),
                        deathHebrewMonth = dHDate?.month ?: HebrewCalendarMath.TISHREI,
                        deathHebrewDay = dHDate?.day?.toString().orEmpty(),
                        deathHebrewFormatted = dFormatted,
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

    private fun syncHebrewFromGregorian(
        yearStr: String,
        monthStr: String,
        dayStr: String,
        precision: DatePrecision,
    ): Pair<HebrewDate?, String> {
        if (precision == DatePrecision.UNKNOWN) return null to ""
        val y = yearStr.toIntOrNull() ?: return null to ""
        if (precision == DatePrecision.YEAR_ONLY) {
            val hYear = y + 3760
            val formatted = HebrewDates.formatHebrewYear(hYear)
            return HebrewDate(hYear, HebrewCalendarMath.TISHREI, 1) to "שנת $formatted"
        }
        val m = monthStr.toIntOrNull() ?: return null to ""
        val d = if (precision == DatePrecision.FULL_DATE) dayStr.toIntOrNull() ?: return null to "" else 1
        val localDate = runCatching { LocalDate.of(y, m, d) }.getOrNull() ?: return null to ""
        val hDate = HebrewCalendarMath.fromLocalDate(localDate)
        val formatted = if (precision == DatePrecision.FULL_DATE) {
            HebrewDates.formatHebrewDate(hDate)
        } else {
            val monthName = HebrewCalendarMath.monthName(hDate.year, hDate.month)
            val yearName = HebrewDates.formatHebrewYear(hDate.year)
            "$monthName $yearName"
        }
        return hDate to formatted
    }

    private fun syncGregorianFromHebrew(
        hYearStr: String,
        hMonth: Int,
        hDayStr: String,
        precision: DatePrecision,
    ): Triple<String, String, String> {
        if (precision == DatePrecision.UNKNOWN) return Triple("", "", "")
        val y = HebrewDates.parseGematria(hYearStr) ?: hYearStr.toIntOrNull() ?: return Triple("", "", "")
        if (precision == DatePrecision.YEAR_ONLY) {
            val gYear = (y - 3760).coerceAtLeast(1)
            return Triple(gYear.toString(), "", "")
        }
        val maxDays = HebrewCalendarMath.monthLength(y, hMonth)
        val d = (HebrewDates.parseGematria(hDayStr) ?: hDayStr.toIntOrNull() ?: 1).coerceIn(1, maxDays)
        val localDate = runCatching { HebrewCalendarMath.toLocalDate(y, hMonth, d) }.getOrNull()
            ?: return Triple("", "", "")
        return Triple(
            localDate.year.toString(),
            localDate.monthValue.toString(),
            if (precision == DatePrecision.FULL_DATE) localDate.dayOfMonth.toString() else "",
        )
    }

    fun onFirstNameChanged(v: String) {
        _uiState.value = _uiState.value.copy(firstName = v, firstNameError = null)
    }

    fun onLastNameChanged(v: String) {
        lastNameEditedByUser = true
        _uiState.value = _uiState.value.copy(lastName = v)
    }

    fun onAdditionalNamesChanged(v: String) {
        _uiState.value = _uiState.value.copy(additionalNames = v)
    }

    fun onPhotoChanged(v: String?) {
        _uiState.value = _uiState.value.copy(photoPath = v)
    }

    fun onLifeStatusChanged(v: LifeStatus) {
        _uiState.value = _uiState.value.copy(lifeStatus = v, dateError = null)
    }

    // --- Birth Date Handlers ---

    fun onBirthCalendarTypeChanged(type: CalendarInputType) {
        _uiState.value = _uiState.value.copy(birthCalendarType = type)
    }

    fun onBirthYearChanged(v: String) {
        val s = _uiState.value
        val (hDate, formatted) = syncHebrewFromGregorian(v, s.birthMonth, s.birthDay, s.birthPrecision)
        _uiState.value = s.copy(
            birthYear = v,
            birthHebrewYear = hDate?.year?.toString() ?: s.birthHebrewYear,
            birthHebrewMonth = hDate?.month ?: s.birthHebrewMonth,
            birthHebrewDay = hDate?.day?.toString() ?: s.birthHebrewDay,
            birthHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onBirthMonthChanged(v: String) {
        val s = _uiState.value
        val (hDate, formatted) = syncHebrewFromGregorian(s.birthYear, v, s.birthDay, s.birthPrecision)
        _uiState.value = s.copy(
            birthMonth = v,
            birthHebrewYear = hDate?.year?.toString() ?: s.birthHebrewYear,
            birthHebrewMonth = hDate?.month ?: s.birthHebrewMonth,
            birthHebrewDay = hDate?.day?.toString() ?: s.birthHebrewDay,
            birthHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onBirthDayChanged(v: String) {
        val s = _uiState.value
        val (hDate, formatted) = syncHebrewFromGregorian(s.birthYear, s.birthMonth, v, s.birthPrecision)
        _uiState.value = s.copy(
            birthDay = v,
            birthHebrewYear = hDate?.year?.toString() ?: s.birthHebrewYear,
            birthHebrewMonth = hDate?.month ?: s.birthHebrewMonth,
            birthHebrewDay = hDate?.day?.toString() ?: s.birthHebrewDay,
            birthHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onBirthPrecisionChanged(v: DatePrecision) {
        val s = _uiState.value
        val (hDate, formatted) = syncHebrewFromGregorian(s.birthYear, s.birthMonth, s.birthDay, v)
        _uiState.value = s.copy(
            birthPrecision = v,
            birthHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onBirthHebrewYearChanged(v: String) {
        val s = _uiState.value
        val (gY, gM, gD) = syncGregorianFromHebrew(v, s.birthHebrewMonth, s.birthHebrewDay, s.birthPrecision)
        val parsedYear = HebrewDates.parseGematria(v) ?: v.toIntOrNull()
        val formatted = if (parsedYear != null) {
            val maxDays = HebrewCalendarMath.monthLength(parsedYear, s.birthHebrewMonth)
            val d = (HebrewDates.parseGematria(s.birthHebrewDay) ?: s.birthHebrewDay.toIntOrNull() ?: 1).coerceIn(1, maxDays)
            HebrewDates.formatHebrewDate(HebrewDate(parsedYear, s.birthHebrewMonth, d))
        } else ""
        _uiState.value = s.copy(
            birthHebrewYear = v,
            birthYear = if (gY.isNotEmpty()) gY else s.birthYear,
            birthMonth = if (gM.isNotEmpty()) gM else s.birthMonth,
            birthDay = if (gD.isNotEmpty()) gD else s.birthDay,
            birthHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onBirthHebrewMonthChanged(month: Int) {
        val s = _uiState.value
        val (gY, gM, gD) = syncGregorianFromHebrew(s.birthHebrewYear, month, s.birthHebrewDay, s.birthPrecision)
        val parsedYear = HebrewDates.parseGematria(s.birthHebrewYear) ?: s.birthHebrewYear.toIntOrNull() ?: 5784
        val maxDays = HebrewCalendarMath.monthLength(parsedYear, month)
        val d = (HebrewDates.parseGematria(s.birthHebrewDay) ?: s.birthHebrewDay.toIntOrNull() ?: 1).coerceIn(1, maxDays)
        val formatted = HebrewDates.formatHebrewDate(HebrewDate(parsedYear, month, d))
        _uiState.value = s.copy(
            birthHebrewMonth = month,
            birthYear = if (gY.isNotEmpty()) gY else s.birthYear,
            birthMonth = if (gM.isNotEmpty()) gM else s.birthMonth,
            birthDay = if (gD.isNotEmpty()) gD else s.birthDay,
            birthHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onBirthHebrewDayChanged(v: String) {
        val s = _uiState.value
        val (gY, gM, gD) = syncGregorianFromHebrew(s.birthHebrewYear, s.birthHebrewMonth, v, s.birthPrecision)
        val parsedYear = HebrewDates.parseGematria(s.birthHebrewYear) ?: s.birthHebrewYear.toIntOrNull() ?: 5784
        val maxDays = HebrewCalendarMath.monthLength(parsedYear, s.birthHebrewMonth)
        val d = (HebrewDates.parseGematria(v) ?: v.toIntOrNull() ?: 1).coerceIn(1, maxDays)
        val formatted = HebrewDates.formatHebrewDate(HebrewDate(parsedYear, s.birthHebrewMonth, d))
        _uiState.value = s.copy(
            birthHebrewDay = v,
            birthYear = if (gY.isNotEmpty()) gY else s.birthYear,
            birthMonth = if (gM.isNotEmpty()) gM else s.birthMonth,
            birthDay = if (gD.isNotEmpty()) gD else s.birthDay,
            birthHebrewFormatted = formatted,
            dateError = null,
        )
    }

    // --- Death Date Handlers ---

    fun onDeathCalendarTypeChanged(type: CalendarInputType) {
        _uiState.value = _uiState.value.copy(deathCalendarType = type)
    }

    fun onDeathYearChanged(v: String) {
        val s = _uiState.value
        val (hDate, formatted) = syncHebrewFromGregorian(v, s.deathMonth, s.deathDay, s.deathPrecision)
        _uiState.value = s.copy(
            deathYear = v,
            deathHebrewYear = hDate?.year?.toString() ?: s.deathHebrewYear,
            deathHebrewMonth = hDate?.month ?: s.deathHebrewMonth,
            deathHebrewDay = hDate?.day?.toString() ?: s.deathHebrewDay,
            deathHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onDeathMonthChanged(v: String) {
        val s = _uiState.value
        val (hDate, formatted) = syncHebrewFromGregorian(s.deathYear, v, s.deathDay, s.deathPrecision)
        _uiState.value = s.copy(
            deathMonth = v,
            deathHebrewYear = hDate?.year?.toString() ?: s.deathHebrewYear,
            deathHebrewMonth = hDate?.month ?: s.deathHebrewMonth,
            deathHebrewDay = hDate?.day?.toString() ?: s.deathHebrewDay,
            deathHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onDeathDayChanged(v: String) {
        val s = _uiState.value
        val (hDate, formatted) = syncHebrewFromGregorian(s.deathYear, s.deathMonth, v, s.deathPrecision)
        _uiState.value = s.copy(
            deathDay = v,
            deathHebrewYear = hDate?.year?.toString() ?: s.deathHebrewYear,
            deathHebrewMonth = hDate?.month ?: s.deathHebrewMonth,
            deathHebrewDay = hDate?.day?.toString() ?: s.deathHebrewDay,
            deathHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onDeathPrecisionChanged(v: DatePrecision) {
        val s = _uiState.value
        val (hDate, formatted) = syncHebrewFromGregorian(s.deathYear, s.deathMonth, s.deathDay, v)
        _uiState.value = s.copy(
            deathPrecision = v,
            deathHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onDeathHebrewYearChanged(v: String) {
        val s = _uiState.value
        val (gY, gM, gD) = syncGregorianFromHebrew(v, s.deathHebrewMonth, s.deathHebrewDay, s.deathPrecision)
        val parsedYear = HebrewDates.parseGematria(v) ?: v.toIntOrNull()
        val formatted = if (parsedYear != null) {
            val maxDays = HebrewCalendarMath.monthLength(parsedYear, s.deathHebrewMonth)
            val d = (HebrewDates.parseGematria(s.deathHebrewDay) ?: s.deathHebrewDay.toIntOrNull() ?: 1).coerceIn(1, maxDays)
            HebrewDates.formatHebrewDate(HebrewDate(parsedYear, s.deathHebrewMonth, d))
        } else ""
        _uiState.value = s.copy(
            deathHebrewYear = v,
            deathYear = if (gY.isNotEmpty()) gY else s.deathYear,
            deathMonth = if (gM.isNotEmpty()) gM else s.deathMonth,
            deathDay = if (gD.isNotEmpty()) gD else s.deathDay,
            deathHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onDeathHebrewMonthChanged(month: Int) {
        val s = _uiState.value
        val (gY, gM, gD) = syncGregorianFromHebrew(s.deathHebrewYear, month, s.deathHebrewDay, s.deathPrecision)
        val parsedYear = HebrewDates.parseGematria(s.deathHebrewYear) ?: s.deathHebrewYear.toIntOrNull() ?: 5784
        val maxDays = HebrewCalendarMath.monthLength(parsedYear, month)
        val d = (HebrewDates.parseGematria(s.deathHebrewDay) ?: s.deathHebrewDay.toIntOrNull() ?: 1).coerceIn(1, maxDays)
        val formatted = HebrewDates.formatHebrewDate(HebrewDate(parsedYear, month, d))
        _uiState.value = s.copy(
            deathHebrewMonth = month,
            deathYear = if (gY.isNotEmpty()) gY else s.deathYear,
            deathMonth = if (gM.isNotEmpty()) gM else s.deathMonth,
            deathDay = if (gD.isNotEmpty()) gD else s.deathDay,
            deathHebrewFormatted = formatted,
            dateError = null,
        )
    }

    fun onDeathHebrewDayChanged(v: String) {
        val s = _uiState.value
        val (gY, gM, gD) = syncGregorianFromHebrew(s.deathHebrewYear, s.deathHebrewMonth, v, s.deathPrecision)
        val parsedYear = HebrewDates.parseGematria(s.deathHebrewYear) ?: s.deathHebrewYear.toIntOrNull() ?: 5784
        val maxDays = HebrewCalendarMath.monthLength(parsedYear, s.deathHebrewMonth)
        val d = (HebrewDates.parseGematria(v) ?: v.toIntOrNull() ?: 1).coerceIn(1, maxDays)
        val formatted = HebrewDates.formatHebrewDate(HebrewDate(parsedYear, s.deathHebrewMonth, d))
        _uiState.value = s.copy(
            deathHebrewDay = v,
            deathYear = if (gY.isNotEmpty()) gY else s.deathYear,
            deathMonth = if (gM.isNotEmpty()) gM else s.deathMonth,
            deathDay = if (gD.isNotEmpty()) gD else s.deathDay,
            deathHebrewFormatted = formatted,
            dateError = null,
        )
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
        if (s.siblingBlocked) return

        _uiState.value = s.copy(isLoading = true, generalError = null)

        viewModelScope.launch {
            val person = Person(
                id = personId ?: "",
                familyId = familyId,
                firstName = s.firstName.trim(),
                lastName = s.lastName.trim().ifBlank { null },
                additionalNames = s.additionalNames.trim().ifBlank { null },
                photoPath = s.photoPath?.trim()?.ifBlank { null },
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
                .onSuccess { saved ->
                    if (personId == null && relation != null) {
                        val linked = createRelationships(saved)
                        if (linked.isFailure) {
                            // Do not leave an orphan card behind (a retry would create a duplicate).
                            personRepository.deletePerson(saved.id)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                generalError = linked.exceptionOrNull()?.localizedMessage ?: "Failed to link relative",
                            )
                            return@launch
                        }
                    }
                    onSuccess()
                }
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
