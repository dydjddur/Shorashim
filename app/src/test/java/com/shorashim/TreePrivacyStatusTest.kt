package com.shorashim

import com.shorashim.core.model.Family
import com.shorashim.core.model.FamilyStatus
import com.shorashim.core.model.MembershipRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreePrivacyStatusTest {

    private fun family(status: FamilyStatus) = Family(
        id = "f", name = "Shitrit", ownerId = "u", status = status,
        currentUserRole = MembershipRole.OWNER, personCount = 5,
    )

    @Test
    fun `only PUBLISHED trees are public`() {
        assertTrue(family(FamilyStatus.PUBLISHED).isPublic)
        listOf(FamilyStatus.DRAFT, FamilyStatus.PRIVATE, FamilyStatus.ARCHIVED).forEach {
            assertTrue(family(it).isPrivate)
            assertFalse(family(it).isPublic)
        }
    }

    @Test
    fun `status update keeps person count and the user's role`() {
        val before = family(FamilyStatus.PUBLISHED)
        // The repository returns the updated row without the member role / person count.
        val fromServer = Family(id = "f", name = "Shitrit", ownerId = "u", status = FamilyStatus.PRIVATE)
        val after = before.withUpdate(fromServer)
        assertEquals(FamilyStatus.PRIVATE, after.status)
        assertEquals(5, after.personCount)
        assertEquals(MembershipRole.OWNER, after.currentUserRole)
        assertTrue(after.canManageMembers)
    }
}
