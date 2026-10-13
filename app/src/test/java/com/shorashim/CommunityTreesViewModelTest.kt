package com.shorashim

import com.shorashim.core.model.Family
import com.shorashim.core.model.PublicFamilyTree
import com.shorashim.data.family.FamilyRepository
import com.shorashim.feature.community.CommunityTreesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityTreesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeCommunityFamilyRepository : FamilyRepository {
        var publicTrees: List<PublicFamilyTree> = emptyList()
        var userSurnames: List<String> = emptyList()
        var shouldFail: Boolean = false

        override suspend fun getPublicTrees(query: String?): Result<List<PublicFamilyTree>> {
            return if (shouldFail) {
                Result.failure(RuntimeException("Database error"))
            } else {
                val q = query?.trim()
                if (q.isNullOrBlank()) {
                    Result.success(publicTrees)
                } else {
                    Result.success(publicTrees.filter { it.name.contains(q, ignoreCase = true) || it.surnames.any { s -> s.contains(q, ignoreCase = true) } })
                }
            }
        }

        override suspend fun getMySurnames(): Result<List<String>> {
            return Result.success(userSurnames)
        }

        override suspend fun getMyFamilies(): Result<List<Family>> = Result.success(emptyList())
        override suspend fun getFamily(familyId: String): Result<Family> = Result.failure(NoSuchElementException())
        override suspend fun createFamily(name: String, description: String?): Result<Family> = Result.failure(UnsupportedOperationException())
        override suspend fun renameFamily(familyId: String, newName: String): Result<Family> = Result.failure(UnsupportedOperationException())
        override suspend fun publishFamily(familyId: String): Result<Family> = Result.failure(UnsupportedOperationException())
        override suspend fun unpublishFamily(familyId: String): Result<Family> = Result.failure(UnsupportedOperationException())
        override suspend fun deleteFamily(familyId: String): Result<Unit> = Result.failure(UnsupportedOperationException())
        override suspend fun getFamilyMembers(familyId: String) = Result.success(emptyList<com.shorashim.core.model.FamilyMember>())
        override suspend fun updateMemberRole(familyId: String, userId: String, role: com.shorashim.core.model.MembershipRole) = Result.success(Unit)
        override suspend fun inviteMember(familyId: String, email: String, role: com.shorashim.core.model.MembershipRole) = Result.success(Unit)
        override suspend fun removeMember(familyId: String, userId: String) = Result.success(Unit)
        override fun observeFamilySummary() = kotlinx.coroutines.flow.emptyFlow<com.shorashim.core.model.FamilySummary>()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val tree1 = PublicFamilyTree(
        id = "t1",
        name = "משפחת וקנין",
        description = "שורשים ממרוקו",
        ownerId = "u1",
        ownerName = "דוד וקנין",
        personCount = 25,
        surnames = listOf("וקנין", "ביטון", "אוחיון"),
    )

    private val tree2 = PublicFamilyTree(
        id = "t2",
        name = "משפחת כהן המורחבת",
        description = "עץ שורשים מירושלים",
        ownerId = "u2",
        ownerName = "משה כהן",
        personCount = 18,
        surnames = listOf("כהן", "לוי"),
    )

    private val tree3 = PublicFamilyTree(
        id = "t3",
        name = "שושלת אברמוביץ",
        description = "עץ מפולין",
        ownerId = "u3",
        ownerName = "שרה",
        personCount = 10,
        surnames = listOf("אברמוביץ", "גולדשטיין", "ביטון"),
    )

    @Test
    fun loadData_populatesPublicTreesAndSurnames() = runTest {
        val repo = FakeCommunityFamilyRepository().apply {
            publicTrees = listOf(tree1, tree2, tree3)
            userSurnames = listOf("וקנין", "דהן")
        }

        val viewModel = CommunityTreesViewModel(repo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(3, state.trees.size)
        assertEquals(3, state.filteredTrees.size)
        assertEquals(listOf("וקנין", "דהן"), state.mySurnames)
        // Check popular surnames (ביטון appears in tree1 and tree3, so it should be near top)
        assertTrue(state.popularSurnames.contains("ביטון"))
        assertTrue(state.popularSurnames.contains("וקנין"))
    }

    @Test
    fun onSearchQueryChanged_filtersByTreeNameAndSurname() = runTest {
        val repo = FakeCommunityFamilyRepository().apply {
            publicTrees = listOf(tree1, tree2, tree3)
            userSurnames = listOf("וקנין")
        }

        val viewModel = CommunityTreesViewModel(repo)
        advanceUntilIdle()

        // Search by tree name
        viewModel.onSearchQueryChanged("כהן")
        assertEquals(1, viewModel.uiState.value.filteredTrees.size)
        assertEquals("t2", viewModel.uiState.value.filteredTrees.first().id)

        // Search by surname that appears in multiple trees ("ביטון")
        viewModel.onSearchQueryChanged("ביטון")
        assertEquals(2, viewModel.uiState.value.filteredTrees.size)
        assertTrue(viewModel.uiState.value.filteredTrees.any { it.id == "t1" })
        assertTrue(viewModel.uiState.value.filteredTrees.any { it.id == "t3" })

        // Clear search
        viewModel.onSearchQueryChanged("")
        assertEquals(3, viewModel.uiState.value.filteredTrees.size)
    }

    @Test
    fun onSelectSurnameFilter_filtersBySpecificSurname() = runTest {
        val repo = FakeCommunityFamilyRepository().apply {
            publicTrees = listOf(tree1, tree2, tree3)
            userSurnames = emptyList()
        }

        val viewModel = CommunityTreesViewModel(repo)
        advanceUntilIdle()

        // Select "אוחיון" chip
        viewModel.onSelectSurnameFilter("אוחיון")
        assertEquals("אוחיון", viewModel.uiState.value.selectedSurnameFilter)
        assertEquals(1, viewModel.uiState.value.filteredTrees.size)
        assertEquals("t1", viewModel.uiState.value.filteredTrees.first().id)

        // Tapping the same chip again deselects it
        viewModel.onSelectSurnameFilter("אוחיון")
        assertNull(viewModel.uiState.value.selectedSurnameFilter)
        assertEquals(3, viewModel.uiState.value.filteredTrees.size)
    }

    @Test
    fun toggleOnlyMySurnamesMatch_filtersOnlyTreesSharingUserSurnames() = runTest {
        val repo = FakeCommunityFamilyRepository().apply {
            publicTrees = listOf(tree1, tree2, tree3)
            userSurnames = listOf("וקנין") // tree1 has "וקנין", tree2 and tree3 do not
        }

        val viewModel = CommunityTreesViewModel(repo)
        advanceUntilIdle()

        // Toggle on
        viewModel.toggleOnlyMySurnamesMatch()
        assertTrue(viewModel.uiState.value.onlyMySurnamesMatch)
        assertEquals(1, viewModel.uiState.value.filteredTrees.size)
        assertEquals("t1", viewModel.uiState.value.filteredTrees.first().id)

        // Toggle off
        viewModel.toggleOnlyMySurnamesMatch()
        assertFalse(viewModel.uiState.value.onlyMySurnamesMatch)
        assertEquals(3, viewModel.uiState.value.filteredTrees.size)
    }

    @Test
    fun clearFilters_resetsAllFilters() = runTest {
        val repo = FakeCommunityFamilyRepository().apply {
            publicTrees = listOf(tree1, tree2, tree3)
            userSurnames = listOf("וקנין")
        }

        val viewModel = CommunityTreesViewModel(repo)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("וקנין")
        viewModel.onSelectSurnameFilter("ביטון")
        viewModel.toggleOnlyMySurnamesMatch()

        viewModel.clearFilters()

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertNull(state.selectedSurnameFilter)
        assertFalse(state.onlyMySurnamesMatch)
        assertEquals(3, state.filteredTrees.size)
    }
}
