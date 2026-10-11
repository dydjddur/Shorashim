package com.shorashim.core.common

import com.shorashim.R
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.model.PartialDate
import com.shorashim.core.model.Relationship
import com.shorashim.core.model.RelationshipType

/**
 * Validation rules and cycle-detection algorithms for Phase 3.
 */
object FamilyValidators {

    /**
     * Validates family name. Must be at least 2 characters (trimmed).
     */
    fun validateFamilyName(name: String): Int? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> R.string.validation_family_name_empty
            trimmed.length < 2 -> R.string.validation_family_name_too_short
            else -> null
        }
    }

    /**
     * Validates person first name. Required and cannot be blank.
     */
    fun validateFirstName(firstName: String): Int? {
        return if (firstName.trim().isEmpty()) {
            R.string.validation_first_name_empty
        } else {
            null
        }
    }

    /**
     * Validates consistency of life status and dates.
     * - If living, death date must not be set.
     * - If deceased with both birth and death years known, death year cannot precede birth year.
     */
    fun validateSingleDate(date: PartialDate?): Int? {
        if (date == null || date.precision == DatePrecision.UNKNOWN) return null
        val y = date.year
        if (y != null && (y < 1 || y > 3000)) {
            return R.string.validation_year_invalid
        }
        val m = date.month
        if (m != null && (m < 1 || m > 12)) {
            return R.string.validation_date_invalid
        }
        val d = date.day
        if (d != null) {
            if (d < 1 || d > 31) return R.string.validation_date_invalid
            if (m != null && y != null) {
                try {
                    java.time.LocalDate.of(y, m, d)
                } catch (e: Exception) {
                    return R.string.validation_date_invalid
                }
            }
        }
        if (date.precision == DatePrecision.FULL_DATE) {
            if (y != null || m != null || d != null) {
                if (y == null || m == null || d == null) {
                    return R.string.validation_date_invalid
                }
            }
        } else if (date.precision == DatePrecision.YEAR_MONTH) {
            if (y != null || m != null) {
                if (y == null || m == null) {
                    return R.string.validation_date_invalid
                }
            }
        }
        return null
    }

    fun validatePersonDates(
        lifeStatus: LifeStatus,
        birthDate: PartialDate?,
        deathDate: PartialDate?,
    ): Int? {
        val bErr = validateSingleDate(birthDate)
        if (bErr != null) return bErr
        val dErr = validateSingleDate(deathDate)
        if (dErr != null) return dErr

        if (lifeStatus == LifeStatus.LIVING) {
            if (deathDate != null && deathDate.precision != DatePrecision.UNKNOWN) {
                return R.string.validation_death_before_birth
            }
        } else if (lifeStatus == LifeStatus.DECEASED) {
            val birthYear = birthDate?.year
            val deathYear = deathDate?.year
            if (birthYear != null && deathYear != null) {
                if (deathYear < birthYear) {
                    return R.string.validation_death_before_birth
                }
                if (deathYear == birthYear) {
                    val birthMonth = birthDate.month
                    val deathMonth = deathDate.month
                    if (birthMonth != null && deathMonth != null) {
                        if (deathMonth < birthMonth) {
                            return R.string.validation_death_before_birth
                        }
                        if (deathMonth == birthMonth) {
                            val birthDay = birthDate.day
                            val deathDay = deathDate.day
                            if (birthDay != null && deathDay != null && deathDay < birthDay) {
                                return R.string.validation_death_before_birth
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Validates relationship before creation:
     * - Cannot link same person to themselves.
     * - Must choose two non-blank person IDs.
     * - Cannot duplicate existing relationship.
     * - For spouse: undirected duplication checked (A-B and B-A are identical).
     * - For parent-child: cycle detection (child cannot already be an ancestor of parent).
     */
    fun validateRelationship(
        personAId: String,
        personBId: String,
        type: RelationshipType,
        existingRelationships: List<Relationship>,
    ): Int? {
        if (personAId.isBlank() || personBId.isBlank()) {
            return R.string.validation_select_both_people
        }
        if (personAId == personBId) {
            return R.string.validation_relationship_same_person
        }

        when (type) {
            RelationshipType.SPOUSE -> {
                val exists = existingRelationships.any { r ->
                    r.relationshipType == RelationshipType.SPOUSE &&
                        ((r.personId == personAId && r.relatedPersonId == personBId) ||
                            (r.personId == personBId && r.relatedPersonId == personAId))
                }
                if (exists) {
                    return R.string.validation_relationship_already_exists
                }
            }
            RelationshipType.PARENT_CHILD -> {
                // personAId is parent, personBId is child
                val exists = existingRelationships.any { r ->
                    r.relationshipType == RelationshipType.PARENT_CHILD &&
                        r.personId == personAId && r.relatedPersonId == personBId
                }
                if (exists) {
                    return R.string.validation_relationship_already_exists
                }

                // Cycle detection: check if personAId is reachable as a descendant of personBId
                if (detectParentChildCycle(parentId = personAId, childId = personBId, relationships = existingRelationships)) {
                    return R.string.validation_relationship_cycle
                }
            }
        }

        return null
    }

    /**
     * Returns true if adding parentId -> childId creates a directed cycle.
     * That is, if parentId is already reachable starting from childId by following PARENT_CHILD edges.
     */
    fun detectParentChildCycle(
        parentId: String,
        childId: String,
        relationships: List<Relationship>,
    ): Boolean {
        if (parentId == childId) return true

        // Build adjacency map: parent -> list of children
        val parentToChildren = mutableMapOf<String, MutableList<String>>()
        for (rel in relationships) {
            if (rel.relationshipType == RelationshipType.PARENT_CHILD) {
                parentToChildren.getOrPut(rel.personId) { mutableListOf() }.add(rel.relatedPersonId)
            }
        }

        // BFS / DFS from childId to check if parentId is reached
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(childId)
        visited.add(childId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == parentId) {
                return true
            }
            val children = parentToChildren[current].orEmpty()
            for (c in children) {
                if (c !in visited) {
                    visited.add(c)
                    queue.add(c)
                }
            }
        }

        return false
    }
}
