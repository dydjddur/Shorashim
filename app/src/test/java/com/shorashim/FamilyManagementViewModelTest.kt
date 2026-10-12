package com.shorashim

import com.shorashim.core.model.Family
import com.shorashim.core.model.FamilyMember
import com.shorashim.core.model.FamilySummary
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
import com.shorashim.feature.family.FamilyManagementViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class TestFamilyRepository : FamilyRepository {
        var families = mutableListOf<Family>()
        var familyMembers = mutableListOf<FamilyMember>()
        var getFamilyFails = false

        override fun observeFamilySummary(): Flow<FamilySummary> = emptyFlow()
        override suspend fun getMyFamilies(): Result<List<Family>> = Result.success(families)
        override suspend fun getFamily(familyId: String): Result<Family> {
            return if (getFamilyFails) {
                Result.failure(IllegalStateException("Access denied"))
            } else {
                families.firstOrNull { it.id == familyId }?.let { Result.success(it) }
                    ?: Result.failure(NoSuchElementException("Family not found"))
            }
        }
        override suspend fun createFamily(name: String, description: String?): Result<Family> =
            Result.success(Family(id = "fam-new", name = name, ownerId = "u-1"))
        override suspend fun publishFamily(familyId: String): Result<Family> =
            Result.success(Family(id = familyId, name = "Published", ownerId = "u-1"))
        override suspend fun unpublishFamily(familyId: String): Result<Family> =
            Result.success(Family(id = familyId, name = "Draft", ownerId = "u-1"))
        override suspend fun renameFamily(familyId: String, newName: String): Result<Family> =
            Result.success(Family(id = familyId, name = newName, ownerId = "u-1"))
        override suspend fun deleteFamily(familyId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getFamilyMembers(familyId: String): Result<List<FamilyMember>> =
            Result.success(familyMembers)
        override suspend fun inviteMember(familyId: String, emailOrUsername: String, role: MembershipRole): Result<Unit> =
            Result.success(Unit)
        override suspend fun updateMemberRole(familyId: String, userId: String, newRole: MembershipRole): Result<Unit> =
            Result.success(Unit)
        override suspend fun removeMember(familyId: String, userId: String): Result<Unit> =
            Result.success(Unit)
    }

    private class TestPersonRepository : PersonRepository {
        var people = mutableListOf<Person>()
        override suspend fun getPeople(familyId: String): Result<List<Person>> = Result.success(people)
        override suspend fun getAllAccessiblePeople(): Result<List<Person>> = Result.success(people)
        override suspend fun getPerson(personId: String): Result<Person> =
            people.firstOrNull { it.id == personId }?.let { Result.success(it) } ?: Result.failure(NoSuchElementException())
        override suspend fun createPerson(person: Person): Result<Person> = Result.success(person)
        override suspend fun updatePerson(person: Person): Result<Person> = Result.success(person)
        override suspend fun searchPeople(familyId: String, query: String): Result<List<Person>> = Result.success(people)
        override suspend fun updatePersonPosition(personId: String, canvasX: Double, canvasY: Double): Result<Unit> = Result.success(Unit)
        override suspend fun deletePerson(personId: String): Result<Unit> = Result.success(Unit)
    }

    private class TestRelationshipRepository : RelationshipRepository {
        var relationships = mutableListOf<Relationship>()
        override suspend fun getRelationships(familyId: String): Result<List<Relationship>> = Result.success(relationships)
        override suspend fun createRelationship(
            familyId: String,
            personId: String,
            relatedPersonId: String,
            type: RelationshipType,
        ): Result<Relationship> =
            Result.success(Relationship(id = "rel-1", familyId = familyId, personId = personId, relatedPersonId = relatedPersonId, relationshipType = type))
        override suspend fun deleteRelationship(relationshipId: String): Result<Unit> = Result.success(Unit)
    }

    private class TestInvitationRepository : InvitationRepository {
        var lastInvitedUsername: String? = null
        var lastInvitedRole: MembershipRole? = null

        override suspend fun createInvitation(familyId: String, expiresInDays: Int, role: MembershipRole): Result<com.shorashim.core.model.FamilyInvitation> =
            Result.failure(NotImplementedError())
        override suspend fun sendInvitationByUsername(familyId: String, username: String, role: MembershipRole): Result<Unit> {
            lastInvitedUsername = username
            lastInvitedRole = role
            return Result.success(Unit)
        }
        override suspend fun getMyPendingInvitations(): Result<List<com.shorashim.core.model.FamilyInvitation>> = Result.success(emptyList())
        override suspend fun getFamilyInvitations(familyId: String): Result<List<com.shorashim.core.model.FamilyInvitation>> = Result.success(emptyList())
        override suspend fun acceptInvitationByToken(token: String): Result<com.shorashim.core.model.InvitationResolution> = Result.failure(NotImplementedError())
        override suspend fun acceptInvitationById(invitationId: String): Result<com.shorashim.core.model.InvitationResolution> = Result.failure(NotImplementedError())
        override suspend fun declineInvitation(invitationId: String): Result<Unit> = Result.success(Unit)
        override suspend fun revokeInvitation(invitationId: String): Result<Unit> = Result.success(Unit)
        override suspend fun resolveInvitationByToken(token: String): Result<com.shorashim.core.model.InvitationResolution> = Result.failure(NotImplementedError())
    }

    private class TestProfileRepository : ProfileRepository {
        var userProfiles = mutableMapOf<String, UserProfile>()
        override suspend fun getProfile(): Result<UserProfile> = Result.failure(NotImplementedError())
        override suspend fun updateProfile(fullName: String, displayName: String): Result<UserProfile> = Result.failure(NotImplementedError())
        override suspend fun searchUserByUsername(username: String): Result<UserProfile?> =
            Result.success(userProfiles[username.lowercase()])
        override suspend fun setUsername(username: String): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun testInitialLoadAndState() = runTest {
        val famRepo = TestFamilyRepository().apply {
            families.add(Family(id = "fam-1", name = "משפחת כהן", ownerId = "u-owner", currentUserRole = MembershipRole.OWNER))
        }
        val personRepo = TestPersonRepository().apply {
            people.add(Person(id = "p-1", familyId = "fam-1", firstName = "יוסי", lastName = "כהן"))
        }
        val relRepo = TestRelationshipRepository()
        val invRepo = TestInvitationRepository()
        val profRepo = TestProfileRepository()

        val vm = FamilyManagementViewModel(
            familyId = "fam-1",
            familyRepository = famRepo,
            personRepository = personRepo,
            relationshipRepository = relRepo,
            invitationRepository = invRepo,
            profileRepository = profRepo,
        )

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("fam-1", state.currentFamilyId)
        assertEquals("משפחת כהן", state.family?.name)
        assertEquals(1, state.people.size)
        assertEquals("יוסי", state.people.first().firstName)
    }

    @Test
    fun testSearchUserByUsername() = runTest {
        val famRepo = TestFamilyRepository()
        val personRepo = TestPersonRepository()
        val relRepo = TestRelationshipRepository()
        val invRepo = TestInvitationRepository()
        val profRepo = TestProfileRepository().apply {
            userProfiles["naor123"] = UserProfile(id = "u-99", username = "naor123", fullName = "נאור לוי", displayName = "נאור")
        }

        val vm = FamilyManagementViewModel(
            familyId = "fam-1",
            familyRepository = famRepo,
            personRepository = personRepo,
            relationshipRepository = relRepo,
            invitationRepository = invRepo,
            profileRepository = profRepo,
        )

        val found = vm.searchUser("@naor123")
        assertNotNull(found)
        assertEquals("naor123", found?.username)
        assertEquals("נאור לוי", found?.fullName)

        val notFound = vm.searchUser("non_existent")
        assertNull(notFound)
    }

    @Test
    fun testInviteMemberByUsername() = runTest {
        val famRepo = TestFamilyRepository()
        val personRepo = TestPersonRepository()
        val relRepo = TestRelationshipRepository()
        val invRepo = TestInvitationRepository()
        val profRepo = TestProfileRepository()

        val vm = FamilyManagementViewModel(
            familyId = "fam-1",
            familyRepository = famRepo,
            personRepository = personRepo,
            relationshipRepository = relRepo,
            invitationRepository = invRepo,
            profileRepository = profRepo,
        )

        vm.inviteMember("@david_cohen", MembershipRole.EDITOR)
        advanceUntilIdle()

        assertEquals("david_cohen", invRepo.lastInvitedUsername)
        assertEquals(MembershipRole.EDITOR, invRepo.lastInvitedRole)
        assertNotNull(vm.uiState.value.toastMessage)
        assertTrue(vm.uiState.value.toastMessage?.contains("david_cohen") == true)
    }

    @Test
    fun testRealtimeSilentRefreshPreservesStateWithoutLoading() = runTest {
        val famRepo = TestFamilyRepository().apply {
            families.add(Family(id = "fam-1", name = "משפחת כהן", ownerId = "u-owner"))
        }
        val personRepo = TestPersonRepository()
        val relRepo = TestRelationshipRepository()
        val invRepo = TestInvitationRepository()
        val profRepo = TestProfileRepository()

        val vm = FamilyManagementViewModel(
            familyId = "fam-1",
            familyRepository = famRepo,
            personRepository = personRepo,
            relationshipRepository = relRepo,
            invitationRepository = invRepo,
            profileRepository = profRepo,
        )
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.people.size)

        // Simulate incoming real-time change: a relative added from another device
        personRepo.people.add(Person(id = "p-2", familyId = "fam-1", firstName = "שרה", lastName = "כהן"))
        vm.refreshDataSilently()
        advanceUntilIdle()

        val updatedState = vm.uiState.value
        // CRUCIAL: isLoading must stay false so canvas viewport & zoom are never reset!
        assertFalse(updatedState.isLoading)
        assertEquals(1, updatedState.people.size)
        assertEquals("שרה", updatedState.people.first().firstName)
    }

    @Test
    fun testAccessRevokedStateOnRealtimeEvent() = runTest {
        val famRepo = TestFamilyRepository().apply {
            families.add(Family(id = "fam-1", name = "משפחת כהן", ownerId = "u-owner"))
        }
        val personRepo = TestPersonRepository()
        val relRepo = TestRelationshipRepository()

        val vm = FamilyManagementViewModel(
            familyId = "fam-1",
            familyRepository = famRepo,
            personRepository = personRepo,
            relationshipRepository = relRepo,
        )
        advanceUntilIdle()

        // Simulate access revoked in DB
        famRepo.getFamilyFails = true
        vm.refreshDataSilently()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.error?.contains("בוטלה") == true)
    }

    @Test
    fun testRefreshDataUpdatesTreeWithoutLoadingSpinner() = runTest {
        val famRepo = TestFamilyRepository().apply {
            families.add(Family(id = "fam-1", name = "משפחת כהן", ownerId = "u-owner"))
        }
        val personRepo = TestPersonRepository()
        val relRepo = TestRelationshipRepository()

        val vm = FamilyManagementViewModel(
            familyId = "fam-1",
            familyRepository = famRepo,
            personRepository = personRepo,
            relationshipRepository = relRepo,
        )
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.people.size)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isRefreshing)

        personRepo.people.add(Person(id = "p-new", familyId = "fam-1", firstName = "יוסי", lastName = "כהן"))
        vm.refreshData(showToast = true)

        assertFalse(vm.uiState.value.isLoading)
        advanceUntilIdle()

        val refreshed = vm.uiState.value
        assertFalse(refreshed.isLoading)
        assertFalse(refreshed.isRefreshing)
        assertEquals(1, refreshed.people.size)
        assertEquals("יוסי", refreshed.people.first().firstName)
        assertEquals("עץ המשפחה עודכן בהצלחה", refreshed.toastMessage)
    }
}
