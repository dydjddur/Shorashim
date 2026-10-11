package com.shorashim.feature.familytree.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.shorashim.core.model.Person
import com.shorashim.core.model.Relationship
import com.shorashim.core.model.RelationshipType

data class TreeNode(
    val person: Person,
    val x: Float,
    val y: Float,
    val width: Float = 180f,
    val height: Float = 95f,
    val generation: Int = 0,
    val spouseIds: List<String> = emptyList(),
    val parentIds: List<String> = emptyList(),
    val childIds: List<String> = emptyList(),
) {
    val center: Offset get() = Offset(x + width / 2f, y + height / 2f)
    val topCenter: Offset get() = Offset(x + width / 2f, y)
    val bottomCenter: Offset get() = Offset(x + width / 2f, y + height)
    val rightCenter: Offset get() = Offset(x + width, y + height / 2f)
    val leftCenter: Offset get() = Offset(x, y + height / 2f)
}

data class TreeConnection(
    val fromNodeId: String,
    val toNodeId: String,
    val type: RelationshipType,
    val start: Offset,
    val end: Offset,
)

/**
 * Deterministic, overlap-free family tree layout.
 *
 * Model
 * -----
 * People joined by SPOUSE relationships form a **household** (one row of cards, spouses side by side).
 * A household hangs under the household of its *host* — the member that has parents in the tree — so a
 * married-in spouse always sits next to the person that "belongs" to the branch.
 *
 * Every household owns a horizontal **band** whose width is
 * `max(width of its own row, total width of its children's bands)`. Bands of siblings never intersect, so a
 * spouse card that is wider than its partner's slot can never land on top of a brother's or sister's card:
 * the whole sibling subtree is simply pushed aside.
 *
 * The layout ignores any stored `canvasX/canvasY` (the map is static — it is not user-arrangeable).
 */
object TreeLayoutEngine {

    const val NODE_WIDTH = 190f
    const val NODE_HEIGHT = 100f
    const val HORIZONTAL_SPACING = 56f
    const val SPOUSE_SPACING = 40f
    const val VERTICAL_SPACING = 100f
    const val TOP_MARGIN = 40f

    private class Household(val index: Int, val members: List<String>) {
        var host: String? = null
        var parentHousehold: Household? = null
        val childHouseholds = mutableListOf<Household>()
        var rowWidth = 0f
        var bandWidth = 0f
        var placed = false
    }

    fun calculateLayout(
        people: List<Person>,
        relationships: List<Relationship>,
    ): Pair<List<TreeNode>, List<TreeConnection>> {
        if (people.isEmpty()) return Pair(emptyList(), emptyList())

        val peopleMap = LinkedHashMap<String, Person>()
        people.forEach { peopleMap.putIfAbsent(it.id, it) }
        val inputOrder = peopleMap.keys.withIndex().associate { it.value to it.index }

        val parentToChildren = LinkedHashMap<String, MutableList<String>>()
        val childToParents = LinkedHashMap<String, MutableList<String>>()
        val spouses = LinkedHashMap<String, MutableList<String>>()
        val spousePairs = LinkedHashSet<Pair<String, String>>()

        for (rel in relationships) {
            val a = rel.personId
            val b = rel.relatedPersonId
            if (a == b || a !in peopleMap || b !in peopleMap) continue
            when (rel.relationshipType) {
                RelationshipType.PARENT_CHILD -> {
                    val kids = parentToChildren.getOrPut(a) { mutableListOf() }
                    if (b !in kids) kids.add(b)
                    val parents = childToParents.getOrPut(b) { mutableListOf() }
                    if (a !in parents) parents.add(a)
                }
                RelationshipType.SPOUSE -> {
                    val key = if (a < b) a to b else b to a
                    if (spousePairs.add(key)) {
                        spouses.getOrPut(a) { mutableListOf() }.add(b)
                        spouses.getOrPut(b) { mutableListOf() }.add(a)
                    }
                }
            }
        }

        val generations = computeGenerations(peopleMap.keys.toList(), parentToChildren, childToParents, spouses)

        // ---- Households (spouse components) ----
        val householdOf = HashMap<String, Household>()
        val households = mutableListOf<Household>()
        for (id in peopleMap.keys) {
            if (id in householdOf) continue
            val component = collectComponent(id, spouses)
            val ordered = orderRow(component, spouses, inputOrder)
            val household = Household(households.size, ordered)
            households.add(household)
            ordered.forEach { householdOf[it] = household }
        }

        // ---- Host + parent household ----
        for (h in households) {
            h.host = h.members.firstOrNull { !childToParents[it].isNullOrEmpty() }
            h.rowWidth = h.members.size * NODE_WIDTH + (h.members.size - 1) * SPOUSE_SPACING
        }
        for (h in households) {
            val host = h.host ?: continue
            val parent = childToParents[host]!!.map { householdOf.getValue(it) }.firstOrNull { it !== h }
            if (parent != null && !createsCycle(h, parent)) {
                h.parentHousehold = parent
                parent.childHouseholds.add(h)
            }
        }
        // Children keep a stable, chronological order.
        for (h in households) {
            h.childHouseholds.sortWith(
                compareBy<Household>(
                    { birthKey(peopleMap[it.host!!]) },
                    { inputOrder.getValue(it.host!!) },
                ),
            )
        }

        // ---- Band widths (post-order, iterative to survive very deep trees) ----
        val roots = households.filter { it.parentHousehold == null }
        val postOrder = postOrder(roots)
        for (h in postOrder) {
            val kids = h.childHouseholds
            val kidsWidth = if (kids.isEmpty()) 0f else kids.sumOf { it.bandWidth.toDouble() }.toFloat() +
                (kids.size - 1) * HORIZONTAL_SPACING
            h.bandWidth = maxOf(h.rowWidth, kidsWidth)
        }

        // ---- Placement (pre-order) ----
        val positions = HashMap<String, Float>() // person id -> left x
        var cursor = 0f
        for (root in roots) {
            place(root, cursor, positions)
            cursor += root.bandWidth + HORIZONTAL_SPACING * 2
        }
        // Households that could not be reached from a root (only possible with corrupt cyclic data).
        for (h in households) {
            if (!h.placed) {
                place(h, cursor, positions)
                cursor += h.bandWidth + HORIZONTAL_SPACING * 2
            }
        }

        // Center the whole drawing around x = 0.
        val minX = positions.values.minOrNull() ?: 0f
        val maxX = positions.values.maxOrNull()?.plus(NODE_WIDTH) ?: 0f
        val shift = -(minX + maxX) / 2f

        val nodeMap = LinkedHashMap<String, TreeNode>()
        for (id in peopleMap.keys) {
            val gen = generations[id] ?: 0
            nodeMap[id] = TreeNode(
                person = peopleMap.getValue(id),
                x = (positions[id] ?: 0f) + shift,
                y = TOP_MARGIN + gen * (NODE_HEIGHT + VERTICAL_SPACING),
                width = NODE_WIDTH,
                height = NODE_HEIGHT,
                generation = gen,
                spouseIds = spouses[id] ?: emptyList(),
                parentIds = childToParents[id] ?: emptyList(),
                childIds = parentToChildren[id] ?: emptyList(),
            )
        }

        val connections = buildConnections(nodeMap, childToParents, spousePairs, householdOf)
        return Pair(nodeMap.values.toList(), connections)
    }

    /** Bounding box of every card, or [Rect.Zero] for an empty layout. */
    fun contentBounds(nodes: List<TreeNode>): Rect {
        if (nodes.isEmpty()) return Rect.Zero
        return Rect(
            left = nodes.minOf { it.x },
            top = nodes.minOf { it.y },
            right = nodes.maxOf { it.x + it.width },
            bottom = nodes.maxOf { it.y + it.height },
        )
    }

    // ------------------------------------------------------------------------------------------------

    private fun computeGenerations(
        ids: List<String>,
        parentToChildren: Map<String, List<String>>,
        childToParents: Map<String, List<String>>,
        spouses: Map<String, List<String>>,
    ): Map<String, Int> {
        val gen = HashMap<String, Int>().apply { ids.forEach { put(it, 0) } }
        val maxPasses = ids.size * 2 + 4

        var pass = 0
        var changed = true
        while (changed && pass < maxPasses) {
            changed = false
            pass++

            // children strictly below parents
            for ((parent, kids) in parentToChildren) {
                val need = gen.getValue(parent) + 1
                for (kid in kids) {
                    if (gen.getValue(kid) < need) {
                        gen[kid] = need
                        changed = true
                    }
                }
            }
            // spouses share a generation
            for ((a, list) in spouses) {
                for (b in list) {
                    val m = maxOf(gen.getValue(a), gen.getValue(b))
                    if (gen.getValue(a) != m) { gen[a] = m; changed = true }
                    if (gen.getValue(b) != m) { gen[b] = m; changed = true }
                }
            }
            // parentless ancestors sit directly above their youngest child (no floating gaps)
            for (id in ids) {
                if (!childToParents[id].isNullOrEmpty()) continue
                val kids = parentToChildren[id] ?: continue
                val target = kids.minOf { gen.getValue(it) } - 1
                if (target > gen.getValue(id)) {
                    gen[id] = target
                    changed = true
                }
            }
        }
        return gen
    }

    private fun collectComponent(start: String, spouses: Map<String, List<String>>): Set<String> {
        val seen = LinkedHashSet<String>()
        val stack = ArrayDeque<String>()
        stack.add(start)
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (!seen.add(id)) continue
            spouses[id]?.forEach { if (it !in seen) stack.add(it) }
        }
        return seen
    }

    /** Orders a spouse component so that every spouse pair that can be adjacent is adjacent. */
    private fun orderRow(
        component: Set<String>,
        spouses: Map<String, List<String>>,
        inputOrder: Map<String, Int>,
    ): List<String> {
        if (component.size <= 2) return component.sortedBy { inputOrder.getValue(it) }
        fun degree(id: String) = spouses[id]?.count { it in component } ?: 0
        val start = component.sortedWith(compareBy({ degree(it) }, { inputOrder.getValue(it) })).first()
        val result = mutableListOf<String>()
        val visited = HashSet<String>()
        fun walk(id: String) {
            if (!visited.add(id)) return
            result.add(id)
            spouses[id].orEmpty().filter { it in component }.sortedBy { inputOrder.getValue(it) }.forEach(::walk)
        }
        walk(start)
        component.filter { it !in visited }.sortedBy { inputOrder.getValue(it) }.forEach { result.add(it) }
        return result
    }

    private fun createsCycle(child: Household, candidateParent: Household): Boolean {
        var cursor: Household? = candidateParent
        var guard = 0
        while (cursor != null && guard++ < 10_000) {
            if (cursor === child) return true
            cursor = cursor.parentHousehold
        }
        return false
    }

    private fun birthKey(person: Person?): String = person?.birthDate?.takeIf { it.isNotBlank() } ?: "9999-99-99"

    private fun postOrder(roots: List<Household>): List<Household> {
        val result = mutableListOf<Household>()
        val stack = ArrayDeque<Pair<Household, Boolean>>()
        roots.asReversed().forEach { stack.add(it to false) }
        while (stack.isNotEmpty()) {
            val (h, expanded) = stack.removeLast()
            if (expanded) {
                result.add(h)
            } else {
                stack.add(h to true)
                h.childHouseholds.asReversed().forEach { stack.add(it to false) }
            }
        }
        return result
    }

    private fun place(root: Household, left: Float, positions: MutableMap<String, Float>) {
        val stack = ArrayDeque<Pair<Household, Float>>()
        stack.add(root to left)
        while (stack.isNotEmpty()) {
            val (h, bandLeft) = stack.removeLast()
            if (h.placed) continue
            h.placed = true

            val rowLeft = bandLeft + (h.bandWidth - h.rowWidth) / 2f
            h.members.forEachIndexed { i, id ->
                positions[id] = rowLeft + i * (NODE_WIDTH + SPOUSE_SPACING)
            }

            val kids = h.childHouseholds
            if (kids.isNotEmpty()) {
                val kidsWidth = kids.sumOf { it.bandWidth.toDouble() }.toFloat() + (kids.size - 1) * HORIZONTAL_SPACING
                var x = bandLeft + (h.bandWidth - kidsWidth) / 2f
                for (kid in kids) {
                    stack.add(kid to x)
                    x += kid.bandWidth + HORIZONTAL_SPACING
                }
            }
        }
    }

    private fun buildConnections(
        nodes: Map<String, TreeNode>,
        childToParents: Map<String, List<String>>,
        spousePairs: Set<Pair<String, String>>,
        householdOf: Map<String, Household>,
    ): List<TreeConnection> {
        val connections = mutableListOf<TreeConnection>()

        for ((a, b) in spousePairs) {
            val nodeA = nodes[a] ?: continue
            val nodeB = nodes[b] ?: continue
            val (left, right) = if (nodeA.x <= nodeB.x) nodeA to nodeB else nodeB to nodeA
            val sameRow = left.y == right.y
            val start = if (sameRow) left.rightCenter else left.bottomCenter
            val end = if (sameRow) right.leftCenter else right.bottomCenter
            connections.add(
                TreeConnection(
                    fromNodeId = a,
                    toNodeId = b,
                    type = RelationshipType.SPOUSE,
                    start = start,
                    end = end,
                ),
            )
        }

        for ((childId, parentIds) in childToParents) {
            val child = nodes[childId] ?: continue
            val remaining = parentIds.filter { it in nodes }.toMutableList()
            while (remaining.isNotEmpty()) {
                val first = remaining.removeAt(0)
                val firstNode = nodes.getValue(first)
                // A couple that sits side by side shares one origin point between their cards.
                val partner = remaining.firstOrNull { other ->
                    val o = nodes.getValue(other)
                    householdOf[first] === householdOf[other] &&
                        o.y == firstNode.y &&
                        kotlin.math.abs(o.x - firstNode.x) <= NODE_WIDTH + SPOUSE_SPACING + 0.5f
                }
                val start = if (partner != null) {
                    remaining.remove(partner)
                    val p = nodes.getValue(partner)
                    Offset((firstNode.bottomCenter.x + p.bottomCenter.x) / 2f, firstNode.bottomCenter.y)
                } else {
                    firstNode.bottomCenter
                }
                connections.add(
                    TreeConnection(
                        fromNodeId = first,
                        toNodeId = childId,
                        type = RelationshipType.PARENT_CHILD,
                        start = start,
                        end = child.topCenter,
                    ),
                )
            }
        }
        return connections
    }
}
