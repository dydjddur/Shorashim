package com.shorashim

import com.shorashim.core.model.Family
import com.shorashim.core.model.FamilyStatus
import com.shorashim.core.model.MembershipRole
import com.shorashim.core.model.Person
import com.shorashim.core.model.Relationship
import com.shorashim.core.model.RelationshipType
import com.shorashim.feature.familytree.canvas.TreeLayoutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeLayoutEngineTest {

    @Test
    fun `empty people produces empty layout`() {
        val (nodes, connections) = TreeLayoutEngine.calculateLayout(emptyList(), emptyList())
        assertTrue(nodes.isEmpty())
        assertTrue(connections.isEmpty())
    }

    @Test
    fun `single person is placed at generation 0`() {
        val person = Person(id = "p1", familyId = "f1", firstName = "Avraham")
        val (nodes, connections) = TreeLayoutEngine.calculateLayout(listOf(person), emptyList())

        assertEquals(1, nodes.size)
        assertEquals(0, nodes[0].generation)
        assertEquals("p1", nodes[0].person.id)
        assertTrue(connections.isEmpty())
    }

    @Test
    fun `parent and child are assigned different generations and have connection`() {
        val parent = Person(id = "parent1", familyId = "f1", firstName = "Yitzhak")
        val child = Person(id = "child1", familyId = "f1", firstName = "Yaakov")
        val rel = Relationship(
            id = "r1",
            familyId = "f1",
            personId = "parent1",
            relatedPersonId = "child1",
            relationshipType = RelationshipType.PARENT_CHILD,
        )

        val (nodes, connections) = TreeLayoutEngine.calculateLayout(listOf(parent, child), listOf(rel))

        assertEquals(2, nodes.size)
        val parentNode = nodes.first { it.person.id == "parent1" }
        val childNode = nodes.first { it.person.id == "child1" }

        assertEquals(0, parentNode.generation)
        assertEquals(1, childNode.generation)
        assertTrue(childNode.y > parentNode.y)

        assertEquals(1, connections.size)
        assertEquals(RelationshipType.PARENT_CHILD, connections[0].type)
        assertEquals("parent1", connections[0].fromNodeId)
        assertEquals("child1", connections[0].toNodeId)
    }

    @Test
    fun `spouses are assigned same generation level`() {
        val spouseA = Person(id = "s1", familyId = "f1", firstName = "Avraham")
        val spouseB = Person(id = "s2", familyId = "f1", firstName = "Sarah")
        val rel = Relationship(
            id = "r1",
            familyId = "f1",
            personId = "s1",
            relatedPersonId = "s2",
            relationshipType = RelationshipType.SPOUSE,
        )

        val (nodes, connections) = TreeLayoutEngine.calculateLayout(listOf(spouseA, spouseB), listOf(rel))

        assertEquals(2, nodes.size)
        val nodeA = nodes.first { it.person.id == "s1" }
        val nodeB = nodes.first { it.person.id == "s2" }

        assertEquals(nodeA.generation, nodeB.generation)
        assertEquals(1, connections.size)
        assertEquals(RelationshipType.SPOUSE, connections[0].type)
    }

    @Test
    fun `RBAC permissions verify role boundaries`() {
        val draftOwnerFamily = Family(
            id = "f1",
            name = "Cohen",
            ownerId = "u1",
            status = FamilyStatus.DRAFT,
            currentUserRole = MembershipRole.OWNER,
        )
        assertTrue(draftOwnerFamily.isDraft)
        assertTrue(draftOwnerFamily.isOwner)
        assertTrue(draftOwnerFamily.canManageMembers)
        assertTrue(draftOwnerFamily.canEditTreeContent)

        val adminFamily = Family(
            id = "f2",
            name = "Levi",
            ownerId = "other_user",
            status = FamilyStatus.PUBLISHED,
            currentUserRole = MembershipRole.ADMIN,
        )
        assertFalse(adminFamily.isDraft)
        assertFalse(adminFamily.isOwner)
        assertTrue(adminFamily.canManageMembers)
        assertTrue(adminFamily.canEditTreeContent)

        val contributorFamily = Family(
            id = "f3",
            name = "Israel",
            ownerId = "other_user",
            status = FamilyStatus.PUBLISHED,
            currentUserRole = MembershipRole.CONTRIBUTOR,
        )
        assertFalse(contributorFamily.isOwner)
        assertFalse(contributorFamily.canManageMembers) // Cannot delete or manage other member roles
        assertTrue(contributorFamily.canEditTreeContent) // Can add and edit relatives!

        val viewerFamily = Family(
            id = "f4",
            name = "Shalom",
            ownerId = "other_user",
            status = FamilyStatus.PUBLISHED,
            currentUserRole = MembershipRole.VIEWER,
        )
        assertFalse(viewerFamily.isOwner)
        assertFalse(viewerFamily.canManageMembers)
        assertFalse(viewerFamily.canEditTreeContent) // Read-only
    }
}
