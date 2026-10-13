package com.shorashim

import com.shorashim.core.model.Family
import com.shorashim.core.model.FamilyStatus
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.model.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonLinkingAndPrivacyTest {

    @Test
    fun testPersonLinkingStatus() {
        val unlinkedPerson = Person(
            id = "p-1",
            familyId = "fam-1",
            firstName = "יוסי",
            lastName = "כהן",
            lifeStatus = LifeStatus.LIVING,
            linkedUserId = null,
        )

        assertFalse(unlinkedPerson.isLinkedToUser)
        assertNull(unlinkedPerson.linkedUserId)

        val linkedPerson = unlinkedPerson.copy(linkedUserId = "user-auth-uuid-999")
        assertTrue(linkedPerson.isLinkedToUser)
        assertEquals("user-auth-uuid-999", linkedPerson.linkedUserId)
    }

    @Test
    fun testFamilyPrivacyStatus() {
        val privateFamily = Family(
            id = "fam-priv",
            name = "משפחת ישראלי הפרטית",
            ownerId = "owner-1",
            status = FamilyStatus.PRIVATE,
        )

        assertTrue(privateFamily.isPrivate)
        assertEquals(FamilyStatus.PRIVATE, privateFamily.status)

        val publicFamily = Family(
            id = "fam-pub",
            name = "משפחת ישראלי הציבורית",
            ownerId = "owner-1",
            status = FamilyStatus.PUBLISHED,
        )

        assertFalse(publicFamily.isPrivate)
        assertEquals(FamilyStatus.PUBLISHED, publicFamily.status)
    }
}
