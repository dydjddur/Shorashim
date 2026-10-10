package com.shorashim

import com.shorashim.core.common.FamilyValidators
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.model.PartialDate
import com.shorashim.core.model.Relationship
import com.shorashim.core.model.RelationshipType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyValidatorsTest {

    @Test
    fun familyNameValidation() {
        assertNotNull(FamilyValidators.validateFamilyName(""))
        assertNotNull(FamilyValidators.validateFamilyName("   "))
        assertNotNull(FamilyValidators.validateFamilyName("א"))
        assertNull(FamilyValidators.validateFamilyName("כהן"))
        assertNull(FamilyValidators.validateFamilyName("משפחת לוי"))
    }

    @Test
    fun personNameValidation() {
        assertNotNull(FamilyValidators.validateFirstName(""))
        assertNotNull(FamilyValidators.validateFirstName("  "))
        assertNull(FamilyValidators.validateFirstName("דן"))
        assertNull(FamilyValidators.validateFirstName("שרה"))
    }

    @Test
    fun personDatesValidation() {
        // Living person cannot have a death date
        val birth = PartialDate(year = 1990, precision = DatePrecision.YEAR_ONLY)
        val death = PartialDate(year = 2020, precision = DatePrecision.YEAR_ONLY)

        assertNotNull(
            FamilyValidators.validatePersonDates(
                lifeStatus = LifeStatus.LIVING,
                birthDate = birth,
                deathDate = death,
            ),
        )

        // Deceased person: death before birth is invalid
        val invalidDeath = PartialDate(year = 1980, precision = DatePrecision.YEAR_ONLY)
        assertNotNull(
            FamilyValidators.validatePersonDates(
                lifeStatus = LifeStatus.DECEASED,
                birthDate = birth,
                deathDate = invalidDeath,
            ),
        )

        // Deceased person: death after birth is valid
        assertNull(
            FamilyValidators.validatePersonDates(
                lifeStatus = LifeStatus.DECEASED,
                birthDate = birth,
                deathDate = death,
            ),
        )

        // Unknown dates are valid
        assertNull(
            FamilyValidators.validatePersonDates(
                lifeStatus = LifeStatus.DECEASED,
                birthDate = PartialDate(precision = DatePrecision.UNKNOWN),
                deathDate = PartialDate(precision = DatePrecision.UNKNOWN),
            ),
        )

        // Invalid month (e.g. 25) is rejected
        val invalidMonth = PartialDate(year = 1980, month = 25, day = 12, precision = DatePrecision.FULL_DATE)
        assertEquals(
            R.string.validation_date_invalid,
            FamilyValidators.validatePersonDates(
                lifeStatus = LifeStatus.LIVING,
                birthDate = invalidMonth,
                deathDate = null,
            ),
        )

        // Invalid day (e.g. 35) is rejected
        val invalidDay = PartialDate(year = 1980, month = 12, day = 35, precision = DatePrecision.FULL_DATE)
        assertEquals(
            R.string.validation_date_invalid,
            FamilyValidators.validatePersonDates(
                lifeStatus = LifeStatus.LIVING,
                birthDate = invalidDay,
                deathDate = null,
            ),
        )
    }

    @Test
    fun partialDateFormatting() {
        val full = PartialDate(year = 1950, month = 5, day = 15, precision = DatePrecision.FULL_DATE)
        assertEquals("1950-05-15", full.toIsoDateString())
        assertEquals("15/05/1950", full.toDisplayString())

        val ym = PartialDate(year = 1960, month = 12, precision = DatePrecision.YEAR_MONTH)
        assertEquals("1960-12-01", ym.toIsoDateString())
        assertEquals("12/1960", ym.toDisplayString())

        val yo = PartialDate(year = 1970, precision = DatePrecision.YEAR_ONLY)
        assertEquals("1970-01-01", yo.toIsoDateString())
        assertEquals("1970", yo.toDisplayString())

        val unk = PartialDate(precision = DatePrecision.UNKNOWN)
        assertNull(unk.toIsoDateString())
        assertEquals("לא ידוע", unk.toDisplayString())
    }

    @Test
    fun relationshipValidationRejectsSelfRelationship() {
        val error = FamilyValidators.validateRelationship(
            personAId = "p1",
            personBId = "p1",
            type = RelationshipType.SPOUSE,
            existingRelationships = emptyList(),
        )
        assertNotNull(error)
    }

    @Test
    fun relationshipValidationRejectsUndirectedDuplicateSpouse() {
        val existing = listOf(
            Relationship(
                id = "r1",
                familyId = "f1",
                personId = "alice",
                relatedPersonId = "bob",
                relationshipType = RelationshipType.SPOUSE,
            ),
        )

        // Exact match
        assertNotNull(
            FamilyValidators.validateRelationship(
                personAId = "alice",
                personBId = "bob",
                type = RelationshipType.SPOUSE,
                existingRelationships = existing,
            ),
        )

        // Reversed match (bob, alice) must also be rejected
        assertNotNull(
            FamilyValidators.validateRelationship(
                personAId = "bob",
                personBId = "alice",
                type = RelationshipType.SPOUSE,
                existingRelationships = existing,
            ),
        )
    }

    @Test
    fun relationshipValidationCycleDetection() {
        // Tree: Grandparent (G) -> Parent (P) -> Child (C)
        val relationships = listOf(
            Relationship(
                id = "r1",
                familyId = "f1",
                personId = "G",
                relatedPersonId = "P",
                relationshipType = RelationshipType.PARENT_CHILD,
            ),
            Relationship(
                id = "r2",
                familyId = "f1",
                personId = "P",
                relatedPersonId = "C",
                relationshipType = RelationshipType.PARENT_CHILD,
            ),
        )

        // Attempting to make Child (C) the parent of Grandparent (G) creates a cycle!
        assertTrue(FamilyValidators.detectParentChildCycle("C", "G", relationships))
        assertNotNull(
            FamilyValidators.validateRelationship(
                personAId = "C",
                personBId = "G",
                type = RelationshipType.PARENT_CHILD,
                existingRelationships = relationships,
            ),
        )

        // Attempting to make Child (C) the parent of Parent (P) creates a cycle!
        assertTrue(FamilyValidators.detectParentChildCycle("C", "P", relationships))

        // Adding an unrelated Person (U) as child of Child (C) is valid!
        assertFalse(FamilyValidators.detectParentChildCycle("C", "U", relationships))
        assertNull(
            FamilyValidators.validateRelationship(
                personAId = "C",
                personBId = "U",
                type = RelationshipType.PARENT_CHILD,
                existingRelationships = relationships,
            ),
        )
    }
}
