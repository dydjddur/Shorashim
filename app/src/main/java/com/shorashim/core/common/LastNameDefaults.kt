package com.shorashim.core.common

import com.shorashim.core.model.NewPersonRelation
import com.shorashim.core.model.Person

/**
 * Suggested family name for a person that is being added to a tree. The user can always overwrite it.
 *
 * Family names are inherited along the father's line, so a new person gets the name from the relative they are
 * attached to rather than blindly from the tree's name. The model has no gender field, so the "father" is
 * inferred: of two parents, the one that already hangs under parents in the tree is the lineage person; the one
 * without parents in the tree is treated as married in.
 */
object LastNameDefaults {

    /**
     * @param treeName name of the family tree (usually the family surname).
     * @param anchor the card the new person is being added from (null when adding a loose person).
     * @param anchorHasParents whether [anchor] has parents in the tree.
     * @param coParent the other parent chosen for a new child (usually the anchor's spouse).
     * @param coParentHasParents whether [coParent] has parents in the tree.
     * @param existingParent for a new *parent*: the anchor's one existing parent, if any.
     */
    fun suggest(
        relation: NewPersonRelation?,
        treeName: String?,
        anchor: Person? = null,
        anchorHasParents: Boolean = false,
        coParent: Person? = null,
        coParentHasParents: Boolean = false,
        existingParent: Person? = null,
    ): String {
        val fallback = treeName?.trim().orEmpty()
        fun nameOf(person: Person?): String = person?.lastName?.trim().takeUnless { it.isNullOrEmpty() } ?: fallback

        return when (relation) {
            null -> fallback
            NewPersonRelation.SPOUSE -> "" // a spouse normally comes from another family
            NewPersonRelation.SIBLING -> nameOf(anchor)
            NewPersonRelation.PARENT ->
                // The second parent of someone who already has a parent is that parent's spouse: a different family.
                if (existingParent != null) "" else nameOf(anchor)
            NewPersonRelation.CHILD -> {
                val lineage = if (coParent != null && coParentHasParents && !anchorHasParents) coParent else anchor
                nameOf(lineage)
            }
        }
    }
}
