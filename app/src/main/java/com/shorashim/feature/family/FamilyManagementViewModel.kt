package com.shorashim.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorashim.core.common.FamilyValidators
import com.shorashim.core.model.Family
import com.shorashim.core.model.FamilyMember
import com.shorashim.core.model.FamilyStatus
import com.shorashim.core.model.MembershipRole
import com.shorashim.core.model.Person
import com.shorashim.core.model.Relationship
import com.shorashim.core.model.RelationshipType
import com.shorashim.data.family.FamilyRepository
import com.shorashim.data.family.PersonRepository
import com.shorashim.data.family.RelationshipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FamilyViewTab {
    CANVAS, // Workflow Map
    LIST,   // Family member list
}

data class FamilyManagementUiState(
    val currentFamilyId: String,
    val family: Family? = null,
    val allFamilies: List<Family> = emptyList(),
    val people: List<Person> = emptyList(),
    val filteredPeople: List<Person> = emptyList(),
    val relationships: List<Relationship> = emptyList(),
    val members: List<FamilyMember> = emptyList(),
    val searchQuery: String = "",
    val activeTab: FamilyViewTab = FamilyViewTab.CANVAS,
    val isLoading: Boolean = false,
    val isRenaming: Boolean = false,
    val isAddRelationshipOpen: Boolean = false,
    val isMembersDialogOpen: Boolean = false,
    val isManagementSheetOpen: Boolean = false,
    val isDeleteFamilyConfirmOpen: Boolean = false,
    val error: String? = null,
    val toastMessage: String? = null,
)

class FamilyManagementViewModel(
    val familyId: String,
    private val familyRepository: FamilyRepository,
    private val personRepository: PersonRepository,
    private val relationshipRepository: RelationshipRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FamilyManagementUiState(
            currentFamilyId = familyId,
            isLoading = true,
        ),
    )
    val uiState: StateFlow<FamilyManagementUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectFamily(newFamilyId: String) {
        if (newFamilyId == _uiState.value.currentFamilyId) return
        _uiState.value = _uiState.value.copy(currentFamilyId = newFamilyId)
        loadData()
    }

    fun setTab(tab: FamilyViewTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun loadData() {
        val fId = _uiState.value.currentFamilyId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val allFamResult = familyRepository.getMyFamilies()
            val famResult = familyRepository.getFamily(fId)
            val peopleResult = personRepository.getPeople(fId)
            val relResult = relationshipRepository.getRelationships(fId)
            val membersResult = familyRepository.getFamilyMembers(fId)

            if (famResult.isSuccess && peopleResult.isSuccess) {
                val fam = famResult.getOrNull()
                val allFams = allFamResult.getOrDefault(emptyList())
                val peopleList = peopleResult.getOrDefault(emptyList())
                val relList = relResult.getOrDefault(emptyList())
                val memberList = membersResult.getOrDefault(emptyList())
                val q = _uiState.value.searchQuery

                val filtered = if (q.isBlank()) peopleList else {
                    peopleList.filter { it.fullName.contains(q, ignoreCase = true) }
                }

                _uiState.value = _uiState.value.copy(
                    family = fam,
                    allFamilies = allFams,
                    people = peopleList,
                    filteredPeople = filtered,
                    relationships = relList,
                    members = memberList,
                    isLoading = false,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = famResult.exceptionOrNull()?.localizedMessage
                        ?: peopleResult.exceptionOrNull()?.localizedMessage
                        ?: "Failed to load family data",
                )
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        val peopleList = _uiState.value.people
        val filtered = if (newQuery.isBlank()) peopleList else {
            peopleList.filter { it.fullName.contains(newQuery, ignoreCase = true) }
        }
        _uiState.value = _uiState.value.copy(
            searchQuery = newQuery,
            filteredPeople = filtered,
        )
    }

    fun publishFamily() {
        val fId = _uiState.value.currentFamilyId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            familyRepository.publishFamily(fId)
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(
                        family = updated,
                        isLoading = false,
                        toastMessage = "העץ פורסם בהצלחה!",
                    )
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = t.localizedMessage)
                }
        }
    }

    fun unpublishFamily() {
        val fId = _uiState.value.currentFamilyId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            familyRepository.unpublishFamily(fId)
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(
                        family = updated,
                        isLoading = false,
                        toastMessage = "העץ הוחזר למצב טיוטה",
                    )
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = t.localizedMessage)
                }
        }
    }

    fun renameFamily(newName: String) {
        val fId = _uiState.value.currentFamilyId
        viewModelScope.launch {
            familyRepository.renameFamily(fId, newName)
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(family = updated, isRenaming = false)
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(error = t.localizedMessage)
                }
        }
    }

    fun inviteMember(email: String, role: MembershipRole) {
        val fId = _uiState.value.currentFamilyId
        viewModelScope.launch {
            familyRepository.inviteMember(fId, email, role)
                .onSuccess {
                    loadMembers()
                    _uiState.value = _uiState.value.copy(toastMessage = "ההזמנה נשלחה בהצלחה!")
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(error = t.localizedMessage)
                }
        }
    }

    fun updateMemberRole(userId: String, role: MembershipRole) {
        val fId = _uiState.value.currentFamilyId
        viewModelScope.launch {
            familyRepository.updateMemberRole(fId, userId, role)
                .onSuccess {
                    loadMembers()
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(error = t.localizedMessage)
                }
        }
    }

    fun removeMember(userId: String) {
        val fId = _uiState.value.currentFamilyId
        viewModelScope.launch {
            familyRepository.removeMember(fId, userId)
                .onSuccess {
                    loadMembers()
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(error = t.localizedMessage)
                }
        }
    }

    private fun loadMembers() {
        val fId = _uiState.value.currentFamilyId
        viewModelScope.launch {
            familyRepository.getFamilyMembers(fId)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(members = list)
                }
        }
    }

    fun deleteFamily(onDeleted: () -> Unit) {
        val fId = _uiState.value.currentFamilyId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            familyRepository.deleteFamily(fId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isDeleteFamilyConfirmOpen = false)
                    onDeleted()
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = t.localizedMessage)
                }
        }
    }

    fun deletePerson(personId: String) {
        viewModelScope.launch {
            personRepository.deletePerson(personId)
                .onSuccess { loadData() }
                .onFailure { t -> _uiState.value = _uiState.value.copy(error = t.localizedMessage) }
        }
    }

    fun setRenaming(open: Boolean) {
        _uiState.value = _uiState.value.copy(isRenaming = open)
    }

    fun setAddRelationshipOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isAddRelationshipOpen = open)
    }

    fun setMembersDialogOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isMembersDialogOpen = open)
    }

    fun setManagementSheetOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isManagementSheetOpen = open)
    }

    fun setDeleteFamilyConfirmOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isDeleteFamilyConfirmOpen = open)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    fun createRelationship(
        personAId: String,
        personBId: String,
        type: RelationshipType,
        onSuccess: () -> Unit,
        onError: (Int) -> Unit,
    ) {
        val validation = FamilyValidators.validateRelationship(
            personAId = personAId,
            personBId = personBId,
            type = type,
            existingRelationships = _uiState.value.relationships,
        )
        if (validation != null) {
            onError(validation)
            return
        }

        viewModelScope.launch {
            relationshipRepository.createRelationship(_uiState.value.currentFamilyId, personAId, personBId, type)
                .onSuccess {
                    loadData()
                    onSuccess()
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(error = t.localizedMessage)
                }
        }
    }

    fun deleteRelationship(relationshipId: String) {
        viewModelScope.launch {
            relationshipRepository.deleteRelationship(relationshipId)
                .onSuccess { loadData() }
        }
    }
}
