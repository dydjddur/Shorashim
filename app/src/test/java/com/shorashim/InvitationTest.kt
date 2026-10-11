package com.shorashim

import com.shorashim.core.model.FamilyInvitation
import com.shorashim.core.model.InvitationResolution
import com.shorashim.core.model.InvitationStatus
import com.shorashim.core.model.MembershipRole
import com.shorashim.data.invitations.InvitationRepository
import com.shorashim.feature.invitations.DeepLinkUiState
import com.shorashim.feature.invitations.InvitationDeepLinkViewModel
import com.shorashim.feature.invitations.InvitationsViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InvitationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun familyInvitation_shareUrl_formatIsCorrect() {
        val invitation = FamilyInvitation(
            id = "inv-1",
            familyId = "fam-1",
            familyName = "משפחת ישראלי",
            token = "abc123token",
            createdBy = "user-1",
            status = InvitationStatus.PENDING,
            role = MembershipRole.CONTRIBUTOR,
        )

        assertEquals("https://shorashim-invite.vercel.app/invite/abc123token", invitation.shareUrl)
        assertTrue(invitation.isPending)
    }

    @Test
    fun invitationDeepLinkViewModel_resolveSuccess_setsResolvedState() = runTest {
        val fakeRepo = FakeInvitationRepository(
            resolutionResult = Result.success(
                InvitationResolution(
                    ok = true,
                    familyId = "fam-1",
                    familyName = "משפחת כהן",
                    invitedBy = "ישראל",
                    role = "CONTRIBUTOR",
                )
            )
        )

        val viewModel = InvitationDeepLinkViewModel("token123", fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DeepLinkUiState.Resolved)
        val resolved = state as DeepLinkUiState.Resolved
        assertEquals("משפחת כהן", resolved.resolution.familyName)
    }

    @Test
    fun invitationDeepLinkViewModel_acceptSuccess_setsAcceptedState() = runTest {
        val fakeRepo = FakeInvitationRepository(
            resolutionResult = Result.success(
                InvitationResolution(ok = true, familyId = "fam-1", familyName = "משפחת כהן")
            ),
            acceptResult = Result.success(
                InvitationResolution(ok = true, familyId = "fam-1", familyName = "משפחת כהן")
            )
        )

        val viewModel = InvitationDeepLinkViewModel("token123", fakeRepo)
        advanceUntilIdle()

        viewModel.accept()
        advanceUntilIdle()

        assertEquals(DeepLinkUiState.Accepted, viewModel.uiState.value)
    }

    @Test
    fun invitationDeepLinkViewModel_resolveExpired_setsErrorState() = runTest {
        val fakeRepo = FakeInvitationRepository(
            resolutionResult = Result.success(
                InvitationResolution(ok = false, error = "invitation_expired")
            )
        )

        val viewModel = InvitationDeepLinkViewModel("expiredToken", fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DeepLinkUiState.Error)
        assertEquals("פג תוקף ההזמנה", (state as DeepLinkUiState.Error).message)
    }

    // --- Fake Test Repository ---
    private class FakeInvitationRepository(
        var resolutionResult: Result<InvitationResolution> = Result.success(InvitationResolution(ok = true)),
        var acceptResult: Result<InvitationResolution> = Result.success(InvitationResolution(ok = true)),
    ) : InvitationRepository {
        override suspend fun createInvitation(
            familyId: String,
            expiresInDays: Int,
            role: MembershipRole,
        ): Result<FamilyInvitation> = Result.success(
            FamilyInvitation(
                id = "inv-1",
                familyId = familyId,
                token = "mock-token",
                createdBy = "user-1",
                status = InvitationStatus.PENDING,
                role = role,
            )
        )

        override suspend fun getMyPendingInvitations(): Result<List<FamilyInvitation>> =
            Result.success(emptyList())

        override suspend fun getFamilyInvitations(familyId: String): Result<List<FamilyInvitation>> =
            Result.success(emptyList())

        override suspend fun acceptInvitationByToken(token: String): Result<InvitationResolution> =
            acceptResult

        override suspend fun acceptInvitationById(invitationId: String): Result<InvitationResolution> =
            acceptResult

        override suspend fun declineInvitation(invitationId: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun revokeInvitation(invitationId: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun resolveInvitationByToken(token: String): Result<InvitationResolution> =
            resolutionResult
    }
}
