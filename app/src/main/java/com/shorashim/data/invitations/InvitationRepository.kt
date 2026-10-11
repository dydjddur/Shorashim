package com.shorashim.data.invitations

import com.shorashim.core.model.FamilyInvitation
import com.shorashim.core.model.InvitationResolution
import com.shorashim.core.model.MembershipRole

interface InvitationRepository {
    /** Create a new invitation link for [familyId]. Returns the shareable token. */
    suspend fun createInvitation(
        familyId: String,
        expiresInDays: Int = 7,
        role: MembershipRole = MembershipRole.CONTRIBUTOR,
    ): Result<FamilyInvitation>

    /** Fetch all pending invitations targeting the current user (by email or user_id). */
    suspend fun getMyPendingInvitations(): Result<List<FamilyInvitation>>

    /** Fetch all invitations for a specific family tree (OWNER/ADMIN only). */
    suspend fun getFamilyInvitations(familyId: String): Result<List<FamilyInvitation>>

    /** Accept the invitation with this token. */
    suspend fun acceptInvitationByToken(token: String): Result<InvitationResolution>

    /** Accept the invitation by its UUID. */
    suspend fun acceptInvitationById(invitationId: String): Result<InvitationResolution>

    /** Decline the invitation by its UUID. */
    suspend fun declineInvitation(invitationId: String): Result<Unit>

    /** Revoke the invitation by its UUID (OWNER/ADMIN only). */
    suspend fun revokeInvitation(invitationId: String): Result<Unit>

    /** Resolve invitation metadata from a public token (before accepting). */
    suspend fun resolveInvitationByToken(token: String): Result<InvitationResolution>
}
