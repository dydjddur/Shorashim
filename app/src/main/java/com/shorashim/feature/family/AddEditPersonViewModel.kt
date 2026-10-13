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
    val deathYear: String = "",
    val deathMonth: String = "",
    val deathDay: String = "",
    val deathPrecision: DatePrecision = DatePrecision.FULL_DATE,
    val deathCalendarType: CalendarInputType = CalendarInputType.GREGORIAN,
    val biography: String = "",
    /** The stored person when editing (needed by the account-linking section). */
    val loadedPerson: Person? = null,
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
    private val profileRepository: com.shorashim.data.profile.ProfileRepository? = null,
    private val friendRepository: com.shorashim.data.friends.FriendRepository? = null,
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

    private fun loadPerson(id: String, showSpinner: Boolean = true) {
        viewModelScope.launch {
            if (showSpinner) _uiState.value = _uiState.value.copy(isLoading = true)
            personRepository.getPerson(id)
                .onSuccess { person ->
                    val bDate = PartialDate.fromIso(person.birthDate, person.birthDatePrecision)
                    val dDate = PartialDate.fromIso(person.deathDate, person.deathDatePrecision)

                    _uiState.value = _uiState.value.copy(
                        isEditing = true,
                        loadedPerson = person,
                        firstName = person.firstName,
                        lastName = person.lastName.orEmpty(),
                        additionalNames = person.additionalNames.orEmpty(),
                        photoPath = person.photoPath,
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

    // --- Linking this record to an app account (moved here from the old details dialog) ---

    suspend fun searchUser(username: String): com.shorashim.core.model.UserProfile? {
        val clean = username.trim().removePrefix("@")
        if (clean.isBlank()) return null
        return profileRepository?.searchUserByUsername(clean)?.getOrNull()
    }

    suspend fun getFriends(): List<com.shorashim.core.model.FriendUser> =
        friendRepository?.getFriends()?.getOrDefault(emptyList()) ?: emptyList()

    suspend fun getMutualConnections(userId: String): List<com.shorashim.core.model.MutualFamilyConnection> =
        personRepository.getMutualFamilyConnections(userId).getOrDefault(emptyList())

    fun linkUser(userId: String?) {
        val id = personId ?: return
        viewModelScope.launch {
            personRepository.linkUserToPerson(id, userId)
                .onSuccess { loadPerson(id, showSpinner = false) }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(generalError = t.localizedMessage ?: "הקישור נכשל")
                }
        }
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

    // --- Dates ---

    fun onBirthCalendarTypeChanged(type: CalendarInputType) {
        _uiState.value = _uiState.value.copy(birthCalendarType = type)
    }

    fun onBirthPrecisionChanged(v: DatePrecision) {
        _uiState.value = _uiState.value.copy(birthPrecision = v, dateError = null)
    }

    /** Called by the date picker; the picker itself works in either calendar and always hands over a Gregorian date. */
    fun onBirthDateChosen(year: Int, month: Int?, day: Int?) {
        _uiState.value = _uiState.value.copy(
            birthYear = year.toString(),
            birthMonth = month?.toString().orEmpty(),
            birthDay = day?.toString().orEmpty(),
            dateError = null,
        )
    }

    fun onDeathCalendarTypeChanged(type: CalendarInputType) {
        _uiState.value = _uiState.value.copy(deathCalendarType = type)
    }

    fun onDeathPrecisionChanged(v: DatePrecision) {
        _uiState.value = _uiState.value.copy(deathPrecision = v, dateError = null)
    }

    fun onDeathDateChosen(year: Int, month: Int?, day: Int?) {
        _uiState.value = _uiState.value.copy(
            deathYear = year.toString(),
            deathMonth = month?.toString().orEmpty(),
            deathDay = day?.toString().orEmpty(),
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
