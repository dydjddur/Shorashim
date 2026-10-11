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

    // ---- Overlap-free, relationship-aware layout ----

    private fun person(id: String, birth: String? = null) =
        Person(id = id, familyId = "f1", firstName = id, birthDate = birth)

    private fun parentChild(parent: String, child: String) = Relationship(
        id = "pc_${parent}_$child", familyId = "f1", personId = parent, relatedPersonId = child,
        relationshipType = RelationshipType.PARENT_CHILD,
    )

    private fun spouse(a: String, b: String) = Relationship(
        id = "sp_${a}_$b", familyId = "f1", personId = a, relatedPersonId = b,
        relationshipType = RelationshipType.SPOUSE,
    )

    private fun assertNoOverlaps(nodes: List<com.shorashim.feature.familytree.canvas.TreeNode>) {
        for (i in nodes.indices) for (j in i + 1 until nodes.size) {
            val a = nodes[i]
            val b = nodes[j]
            val overlapX = a.x < b.x + b.width && b.x < a.x + a.width
            val overlapY = a.y < b.y + b.height && b.y < a.y + a.height
            assertFalse("${a.person.id} overlaps ${b.person.id}", overlapX && overlapY)
        }
    }

    @Test
    fun `married-in spouse never lands on a sibling card`() {
        // Dad+Mom -> A, B, C. A marries W: W must be pushed beside A, never on top of B.
        val people = listOf("dad", "mom", "a", "b", "c", "w").map { person(it) }
        val rels = listOf(
            spouse("dad", "mom"),
            parentChild("dad", "a"), parentChild("mom", "a"),
            parentChild("dad", "b"), parentChild("mom", "b"),
            parentChild("dad", "c"), parentChild("mom", "c"),
            spouse("a", "w"),
        )
        val (nodes, _) = TreeLayoutEngine.calculateLayout(people, rels)
        assertNoOverlaps(nodes)

        val byId = nodes.associateBy { it.person.id }
        assertEquals(byId.getValue("a").generation, byId.getValue("w").generation)
        val gap = kotlin.math.abs(byId.getValue("a").x - byId.getValue("w").x)
        assertEquals(TreeLayoutEngine.NODE_WIDTH + TreeLayoutEngine.SPOUSE_SPACING, gap, 0.01f)
    }

    @Test
    fun `grandchildren of one sibling do not collide with the next sibling family`() {
        val people = listOf("p", "a", "aw", "k1", "k2", "k3", "b", "bw", "k4").map { person(it) }
        val rels = listOf(
            parentChild("p", "a"), parentChild("p", "b"),
            spouse("a", "aw"), spouse("b", "bw"),
            parentChild("a", "k1"), parentChild("aw", "k1"),
            parentChild("a", "k2"), parentChild("a", "k3"),
            parentChild("b", "k4"), parentChild("bw", "k4"),
        )
        val (nodes, _) = TreeLayoutEngine.calculateLayout(people, rels)
        assertEquals(people.size, nodes.size)
        assertNoOverlaps(nodes)
    }

    @Test
    fun `couple children share one origin point between the parents`() {
        val people = listOf("f", "m", "k").map { person(it) }
        val rels = listOf(spouse("f", "m"), parentChild("f", "k"), parentChild("m", "k"))
        val (nodes, connections) = TreeLayoutEngine.calculateLayout(people, rels)
        val parentLinks = connections.filter { it.type == RelationshipType.PARENT_CHILD }
        assertEquals(1, parentLinks.size)
        val f = nodes.first { it.person.id == "f" }
        val m = nodes.first { it.person.id == "m" }
        assertEquals((f.bottomCenter.x + m.bottomCenter.x) / 2f, parentLinks[0].start.x, 0.01f)
    }

    @Test
    fun `children are ordered by birth date`() {
        val people = listOf(person("p"), person("young", "2010-01-01"), person("old", "1990-01-01"))
        val rels = listOf(parentChild("p", "young"), parentChild("p", "old"))
        val (nodes, _) = TreeLayoutEngine.calculateLayout(people, rels)
        assertTrue(nodes.first { it.person.id == "old" }.x < nodes.first { it.person.id == "young" }.x)
    }

    @Test
    fun `stored canvas coordinates are ignored because the map is static`() {
        val moved = Person(id = "m", familyId = "f1", firstName = "m", canvasX = 9999.0, canvasY = 9999.0)
        val (nodes, _) = TreeLayoutEngine.calculateLayout(listOf(moved), emptyList())
        assertTrue(nodes[0].x < 1000f)
        assertTrue(nodes[0].y < 1000f)
    }

    @Test
    fun `cyclic or duplicate data still terminates without losing people`() {
        val people = listOf(person("a"), person("b"), person("c"))
        val rels = listOf(
            parentChild("a", "b"), parentChild("b", "c"), parentChild("c", "a"),
            spouse("a", "b"), spouse("b", "a"), spouse("a", "a"),
            parentChild("ghost", "a"),
        )
        val (nodes, _) = TreeLayoutEngine.calculateLayout(people, rels)
        assertEquals(3, nodes.size)
    }

    @Test
    fun `parentless ancestor is lowered next to the generation of its child`() {
        // gp -> dad ; dad + mom -> kid ; mom has her own parents (mgp) who must sit one row above mom.
        val people = listOf("gp", "dad", "mom", "mgp", "kid").map { person(it) }
        val rels = listOf(
            parentChild("gp", "dad"), spouse("dad", "mom"), parentChild("mgp", "mom"),
            parentChild("dad", "kid"), parentChild("mom", "kid"),
        )
        val (nodes, _) = TreeLayoutEngine.calculateLayout(people, rels)
        val gen = nodes.associate { it.person.id to it.generation }
        assertEquals(gen.getValue("dad"), gen.getValue("mom"))
        assertEquals(gen.getValue("mom") - 1, gen.getValue("mgp"))
        assertNoOverlaps(nodes)
    }
}
