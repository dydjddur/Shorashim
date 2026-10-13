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
import com.shorashim.core.model.UserProfile
import com.shorashim.data.family.FamilyRepository
import com.shorashim.data.family.PersonRepository
import com.shorashim.data.family.RelationshipRepository
import com.shorashim.data.invitations.InvitationRepository
import com.shorashim.data.profile.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
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
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val toastMessage: String? = null,
)

class FamilyManagementViewModel(
    val familyId: String,
    private val familyRepository: FamilyRepository,
    private val personRepository: PersonRepository,
    private val relationshipRepository: RelationshipRepository,
    private val invitationRepository: InvitationRepository? = null,
    private val profileRepository: ProfileRepository? = null,
    private val friendRepository: com.shorashim.data.friends.FriendRepository? = null,
    private val supabaseClient: SupabaseClient? = null,
) : ViewModel() {

    private var realtimeChannel: RealtimeChannel? = null
    private val realtimeJobs = mutableListOf<Job>()

    private val _uiState = MutableStateFlow(
        FamilyManagementUiState(
            currentFamilyId = familyId,
            isLoading = true,
        ),
    )
    val uiState: StateFlow<FamilyManagementUiState> = _uiState.asStateFlow()

    init {
        loadData()
        setupRealtime(familyId)
    }

    fun selectFamily(newFamilyId: String) {
        if (newFamilyId == _uiState.value.currentFamilyId) return
        _uiState.value = _uiState.value.copy(currentFamilyId = newFamilyId)
        loadData()
        setupRealtime(newFamilyId)
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
                val rawFams = allFamResult.getOrDefault(emptyList())
                val allFams = if (fam != null && rawFams.none { it.id == fam.id }) {
                    rawFams + fam
                } else {
                    rawFams
                }
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

    fun refreshData(showToast: Boolean = true) {
        val fId = _uiState.value.currentFamilyId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val allFamResult = familyRepository.getMyFamilies()
            val famResult = familyRepository.getFamily(fId)
            val peopleResult = personRepository.getPeople(fId)
            val relResult = relationshipRepository.getRelationships(fId)
            val membersResult = familyRepository.getFamilyMembers(fId)

            if (famResult.isSuccess && peopleResult.isSuccess) {
                val fam = famResult.getOrNull()
                val rawFams = allFamResult.getOrDefault(emptyList())
                val allFams = if (fam != null && rawFams.none { it.id == fam.id }) {
                    rawFams + fam
                } else {
                    rawFams
                }
                val peopleList = peopleResult.getOrDefault(emptyList())
                val relList = relResult.getOrDefault(emptyList())
                val memberList = membersResult.getOrDefault(emptyList())
                val q = _uiState.value.searchQuery

                val filtered = if (q.isBlank()) peopleList else {
                    peopleList.filter { it.fullName.contains(q, ignoreCase = true) }
                }

                _uiState.value = _uiState.value.copy(
                    family = fam ?: _uiState.value.family,
                    allFamilies = if (allFams.isNotEmpty()) allFams else _uiState.value.allFamilies,
                    people = peopleList,
                    filteredPeople = filtered,
                    relationships = relList,
                    members = memberList,
                    isRefreshing = false,
                    toastMessage = if (showToast) "עץ המשפחה עודכן בהצלחה" else _uiState.value.toastMessage,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = famResult.exceptionOrNull()?.localizedMessage
                        ?: peopleResult.exceptionOrNull()?.localizedMessage,
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
                        toastMessage = "העץ הוגדר כפרטי (PRIVATE) ומוסתר מהקהילה",
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

    suspend fun searchUser(username: String): UserProfile? {
        val clean = username.trim().removePrefix("@")
        if (clean.isBlank()) return null
        return profileRepository?.searchUserByUsername(clean)?.getOrNull()
    }

    suspend fun getFriends(): List<com.shorashim.core.model.FriendUser> {
        return friendRepository?.getFriends()?.getOrDefault(emptyList()) ?: emptyList()
    }

    fun linkUserToPerson(personId: String, userId: String) {
        viewModelScope.launch {
            personRepository.linkUserToPerson(personId, userId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(toastMessage = "האדם קושר בהצלחה לחשבון המשתמש!")
                    refreshData(showToast = false)
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(error = t.localizedMessage)
                }
        }
    }

    fun unlinkUserFromPerson(personId: String) {
        viewModelScope.launch {
            personRepository.linkUserToPerson(personId, null)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(toastMessage = "הקישור לחשבון המשתמש בוטל בהצלחה")
                    refreshData(showToast = false)
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(error = t.localizedMessage)
                }
        }
    }

    suspend fun getMutualConnections(userId: String): List<com.shorashim.core.model.MutualFamilyConnection> {
        return personRepository.getMutualFamilyConnections(userId).getOrDefault(emptyList())
    }

    fun inviteMember(username: String, role: MembershipRole) {
        val fId = _uiState.value.currentFamilyId
        val clean = username.trim().removePrefix("@")
        if (clean.isBlank()) return

        viewModelScope.launch {
            val repo = invitationRepository
            if (repo == null) {
                familyRepository.inviteMember(fId, clean, role)
                    .onSuccess {
                        loadMembers()
                        _uiState.value = _uiState.value.copy(toastMessage = "ההזמנה נשלחה בהצלחה!")
                    }
                    .onFailure { t ->
                        _uiState.value = _uiState.value.copy(error = t.localizedMessage)
                    }
                return@launch
            }

            repo.sendInvitationByUsername(fId, clean, role)
                .onSuccess {
                    loadMembers()
                    _uiState.value = _uiState.value.copy(toastMessage = "ההזמנה נשלחה בהצלחה ל-@$clean!")
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(error = t.localizedMessage ?: "שליחת ההזמנה נכשלה")
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

    private fun setupRealtime(fId: String) {
        teardownRealtime()
        val client = supabaseClient ?: return

        viewModelScope.launch {
            try {
                val channel = client.realtime.channel("tree_$fId")
                realtimeChannel = channel

                val peopleFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "people"
                    filter(FilterOperation("family_id", FilterOperator.EQ, fId))
                }
                val peopleJob = launch {
                    peopleFlow.collect {
                        refreshDataSilently()
                    }
                }
                realtimeJobs.add(peopleJob)

                val relFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "relationships"
                    filter(FilterOperation("family_id", FilterOperator.EQ, fId))
                }
                val relJob = launch {
                    relFlow.collect {
                        refreshDataSilently()
                    }
                }
                realtimeJobs.add(relJob)

                val membersFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "family_members"
                    filter(FilterOperation("family_id", FilterOperator.EQ, fId))
                }
                val membersJob = launch {
                    membersFlow.collect {
                        refreshDataSilently()
                    }
                }
                realtimeJobs.add(membersJob)

                channel.subscribe()
            } catch (t: Throwable) {
                android.util.Log.w("FamilyMgmtVM", "Failed to setup realtime: ${t.message}")
            }
        }
    }

    internal fun refreshDataSilently() {
        val fId = _uiState.value.currentFamilyId
        viewModelScope.launch {
            val famResult = familyRepository.getFamily(fId)
            val peopleResult = personRepository.getPeople(fId)
            val relResult = relationshipRepository.getRelationships(fId)
            val membersResult = familyRepository.getFamilyMembers(fId)

            if (famResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = "הגישה לעץ זה בוטלה או שהעץ אינו קיים",
                )
                return@launch
            }

            val fam = famResult.getOrNull()
            val peopleList = peopleResult.getOrDefault(emptyList())
            val relList = relResult.getOrDefault(emptyList())
            val memberList = membersResult.getOrDefault(emptyList())
            val q = _uiState.value.searchQuery

            val filtered = if (q.isBlank()) peopleList else {
                peopleList.filter { it.fullName.contains(q, ignoreCase = true) }
            }

            _uiState.value = _uiState.value.copy(
                family = fam,
                people = peopleList,
                filteredPeople = filtered,
                relationships = relList,
                members = memberList,
            )
        }
    }

    private fun teardownRealtime() {
        realtimeJobs.forEach { it.cancel() }
        realtimeJobs.clear()
        val client = supabaseClient
        val channel = realtimeChannel
        if (client != null && channel != null) {
            viewModelScope.launch {
                try {
                    client.realtime.removeChannel(channel)
                } catch (_: Throwable) {}
            }
        }
        realtimeChannel = null
    }

    override fun onCleared() {
        super.onCleared()
        teardownRealtime()
    }
}
