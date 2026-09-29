package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import

/**
 * Directed graph over arbitrary string node identifiers (unit value codes, or "groupId:code"
 * keys - callers pick the identifier scheme). Used both to reject cyclic base-unit/conversion
 * chains and, once a payload is known to be acyclic, to produce a parent-before-child save order.
 */
class UnitValueDependencyGraph {

    private val adjacency = mutableMapOf<String, MutableSet<String>>()

    fun addNode(node: String) {
        adjacency.getOrPut(node) { mutableSetOf() }
    }

    fun addEdge(fromNode: String, toNode: String) {
        addNode(fromNode)
        addNode(toNode)
        adjacency.getValue(fromNode).add(toNode)
    }

    /** Returns one cycle as an ordered node list (first == last), or null if the graph is acyclic. */
    fun findCycle(): List<String>? {
        val visited = mutableSetOf<String>()
        val onStack = mutableSetOf<String>()
        val path = mutableListOf<String>()

        fun visit(node: String): List<String>? {
            visited.add(node)
            onStack.add(node)
            path.add(node)
            for (neighbour in adjacency.getValue(node)) {
                if (neighbour in onStack) {
                    val cycleStart = path.indexOf(neighbour)
                    return path.subList(cycleStart, path.size) + neighbour
                }
                if (neighbour !in visited) {
                    visit(neighbour)?.let { return it }
                }
            }
            onStack.remove(node)
            path.removeAt(path.size - 1)
            return null
        }

        for (node in adjacency.keys) {
            if (node !in visited) {
                visit(node)?.let { return it }
            }
        }
        return null
    }

    /** Kahn's algorithm. Only valid to call once findCycle() has confirmed the graph is acyclic. */
    fun topologicalOrder(): List<String> {
        val inDegree = adjacency.keys.associateWith { 0 }.toMutableMap()
        adjacency.values.forEach { targets -> targets.forEach { inDegree[it] = inDegree.getValue(it) + 1 } }

        val queue = ArrayDeque(inDegree.filterValues { it == 0 }.keys)
        val order = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            order.add(node)
            adjacency.getValue(node).forEach { neighbour ->
                inDegree[neighbour] = inDegree.getValue(neighbour) - 1
                if (inDegree.getValue(neighbour) == 0) queue.add(neighbour)
            }
        }
        return order
    }
}
