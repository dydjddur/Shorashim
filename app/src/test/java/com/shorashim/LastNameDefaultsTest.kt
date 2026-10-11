package com.shorashim

import com.shorashim.core.common.LastNameDefaults
import com.shorashim.core.model.NewPersonRelation
import com.shorashim.core.model.Person
import org.junit.Assert.assertEquals
import org.junit.Test

class LastNameDefaultsTest {

    private fun person(id: String, last: String?) = Person(id = id, familyId = "f", firstName = id, lastName = last)

    @Test
    fun `a loose person gets the tree name`() {
        assertEquals("Shitrit", LastNameDefaults.suggest(null, "Shitrit"))
    }

    @Test
    fun `a child inherits the surname of the card it is added from, not the tree name`() {
        val father = person("dad", "Levi")
        assertEquals("Levi", LastNameDefaults.suggest(NewPersonRelation.CHILD, "Shitrit", anchor = father))
    }

    @Test
    fun `a child of a lineage parent and a married-in spouse takes the lineage surname`() {
        // The user taps the married-in mother, but the father (who hangs under parents in the tree) passes the name.
        val mother = person("mom", "Cohen")
        val father = person("dad", "Levi")
        val suggestion = LastNameDefaults.suggest(
            relation = NewPersonRelation.CHILD,
            treeName = "Shitrit",
            anchor = mother,
            anchorHasParents = false,
            coParent = father,
            coParentHasParents = true,
        )
        assertEquals("Levi", suggestion)
    }

    @Test
    fun `when both or neither parent are in the tree the tapped card wins`() {
        val a = person("a", "Levi")
        val b = person("b", "Cohen")
        assertEquals("Levi", LastNameDefaults.suggest(NewPersonRelation.CHILD, "T", a, true, b, true))
        assertEquals("Levi", LastNameDefaults.suggest(NewPersonRelation.CHILD, "T", a, false, b, false))
    }

    @Test
    fun `siblings share the surname and a new parent defaults to the childs surname`() {
        val kid = person("kid", "Levi")
        assertEquals("Levi", LastNameDefaults.suggest(NewPersonRelation.SIBLING, "T", kid))
        assertEquals("Levi", LastNameDefaults.suggest(NewPersonRelation.PARENT, "T", kid))
    }

    @Test
    fun `the second parent and a spouse start blank because they come from another family`() {
        val kid = person("kid", "Levi")
        val existing = person("dad", "Levi")
        assertEquals("", LastNameDefaults.suggest(NewPersonRelation.PARENT, "T", kid, existingParent = existing))
        assertEquals("", LastNameDefaults.suggest(NewPersonRelation.SPOUSE, "T", kid))
    }

    @Test
    fun `missing surnames fall back to the tree name`() {
        val nameless = person("x", null)
        assertEquals("Shitrit", LastNameDefaults.suggest(NewPersonRelation.CHILD, "Shitrit", nameless))
        assertEquals("Shitrit", LastNameDefaults.suggest(NewPersonRelation.SIBLING, " Shitrit ", nameless))
    }
}
