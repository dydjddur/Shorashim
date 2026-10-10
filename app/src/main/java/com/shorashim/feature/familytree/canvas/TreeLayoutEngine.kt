package com.shorashim.feature.familytree.canvas

import androidx.compose.ui.geometry.Offset
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

object TreeLayoutEngine {

    const val NODE_WIDTH = 190f
    const val NODE_HEIGHT = 100f
    const val HORIZONTAL_SPACING = 50f
    const val VERTICAL_SPACING = 90f

    fun calculateLayout(
        people: List<Person>,
        relationships: List<Relationship>,
    ): Pair<List<TreeNode>, List<TreeConnection>> {
        if (people.isEmpty()) return Pair(emptyList(), emptyList())

        val peopleMap = people.associateBy { it.id }

        // Maps for fast relationship lookups
        val parentToChildren = mutableMapOf<String, MutableList<String>>()
        val childToParents = mutableMapOf<String, MutableList<String>>()
        val spouses = mutableMapOf<String, MutableList<String>>()

        for (rel in relationships) {
            when (rel.relationshipType) {
                RelationshipType.PARENT_CHILD -> {
                    // personId is parent, relatedPersonId is child
                    parentToChildren.getOrPut(rel.personId) { mutableListOf() }.add(rel.relatedPersonId)
                    childToParents.getOrPut(rel.relatedPersonId) { mutableListOf() }.add(rel.personId)
                }
                RelationshipType.SPOUSE -> {
                    spouses.getOrPut(rel.personId) { mutableListOf() }.add(rel.relatedPersonId)
                    spouses.getOrPut(rel.relatedPersonId) { mutableListOf() }.add(rel.personId)
                }
            }
        }

        // 1. Determine generation level for each person
        val generations = mutableMapOf<String, Int>()

        // Identify roots (people with no parents in the dataset)
        val roots = people.filter { childToParents[it.id].isNullOrEmpty() }
        val queue = ArrayDeque<Pair<String, Int>>()

        if (roots.isEmpty()) {
            // In case of cycles or isolated groups, pick the first person as generation 0
            people.firstOrNull()?.let { queue.add(it.id to 0) }
        } else {
            roots.forEach { queue.add(it.id to 0) }
        }

        while (queue.isNotEmpty()) {
            val (currentId, gen) = queue.removeFirst()
            val existingGen = generations[currentId]
            if (existingGen == null || gen > existingGen) {
                generations[currentId] = gen

                // Spouse has same generation
                spouses[currentId]?.forEach { spouseId ->
                    if (!generations.containsKey(spouseId) || generations[spouseId]!! < gen) {
                        generations[spouseId] = gen
                    }
                }

                // Children have gen + 1
                parentToChildren[currentId]?.forEach { childId ->
                    val childGen = gen + 1
                    if (!generations.containsKey(childId) || generations[childId]!! < childGen) {
                        queue.add(childId to childGen)
                    }
                }
            }
        }

        // Assign gen 0 to any remaining disconnected individuals
        people.forEach { p ->
            if (!generations.containsKey(p.id)) {
                generations[p.id] = 0
            }
        }

        // 2. Group people by generation
        val byGeneration = people.groupBy { generations[it.id] ?: 0 }
        val sortedGenKeys = byGeneration.keys.sorted()

        // 3. Compute (x, y) coordinates
        val nodeMap = mutableMapOf<String, TreeNode>()

        var currentY = 60f

        for (gen in sortedGenKeys) {
            val genPeople = byGeneration[gen] ?: emptyList()

            // Group couples together side-by-side
            val placedInGen = mutableSetOf<String>()
            val orderedInGen = mutableListOf<Person>()

            for (p in genPeople) {
                if (p.id in placedInGen) continue
                orderedInGen.add(p)
                placedInGen.add(p.id)

                // Place spouses immediately next to each other
                spouses[p.id]?.forEach { spouseId ->
                    val spouse = peopleMap[spouseId]
                    if (spouse != null && spouse.id !in placedInGen && generations[spouse.id] == gen) {
                        orderedInGen.add(spouse)
                        placedInGen.add(spouse.id)
                    }
                }
            }

            val totalWidth = orderedInGen.size * NODE_WIDTH + (orderedInGen.size - 1).coerceAtLeast(0) * HORIZONTAL_SPACING
            var currentX = -totalWidth / 2f

            for (p in orderedInGen) {
                // If the person has custom canvas position, allow it or use auto-layout
                val finalX = if (p.canvasX != 0.0) p.canvasX.toFloat() else currentX
                val finalY = if (p.canvasY != 0.0) p.canvasY.toFloat() else currentY

                val treeNode = TreeNode(
                    person = p,
                    x = finalX,
                    y = finalY,
                    width = NODE_WIDTH,
                    height = NODE_HEIGHT,
                    generation = gen,
                    spouseIds = spouses[p.id] ?: emptyList(),
                    parentIds = childToParents[p.id] ?: emptyList(),
                    childIds = parentToChildren[p.id] ?: emptyList(),
                )

                nodeMap[p.id] = treeNode
                currentX += NODE_WIDTH + HORIZONTAL_SPACING
            }

            currentY += NODE_HEIGHT + VERTICAL_SPACING
        }

        // 4. Compute connection lines
        val connections = mutableListOf<TreeConnection>()

        for (rel in relationships) {
            val nodeA = nodeMap[rel.personId]
            val nodeB = nodeMap[rel.relatedPersonId]

            if (nodeA != null && nodeB != null) {
                when (rel.relationshipType) {
                    RelationshipType.PARENT_CHILD -> {
                        // From bottom of parent to top of child
                        connections.add(
                            TreeConnection(
                                fromNodeId = rel.personId,
                                toNodeId = rel.relatedPersonId,
                                type = RelationshipType.PARENT_CHILD,
                                start = nodeA.bottomCenter,
                                end = nodeB.topCenter,
                            ),
                        )
                    }
                    RelationshipType.SPOUSE -> {
                        // Between sides of the spouse cards
                        val isALeft = nodeA.x < nodeB.x
                        val startPt = if (isALeft) nodeA.rightCenter else nodeA.leftCenter
                        val endPt = if (isALeft) nodeB.leftCenter else nodeB.rightCenter
                        connections.add(
                            TreeConnection(
                                fromNodeId = rel.personId,
                                toNodeId = rel.relatedPersonId,
                                type = RelationshipType.SPOUSE,
                                start = startPt,
                                end = endPt,
                            ),
                        )
                    }
                }
            }
        }

        return Pair(nodeMap.values.toList(), connections)
    }
}
