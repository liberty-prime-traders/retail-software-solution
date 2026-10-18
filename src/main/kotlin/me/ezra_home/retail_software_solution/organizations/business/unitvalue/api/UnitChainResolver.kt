package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.util.UUID

data class UnitChainNode(val id: UUID, val baseUnitId: UUID?, val unitsOfBasePerUnit: Long?)

data class UnitPlacement(val rootUnitId: UUID, val sizeInRoot: Long)

object UnitChainResolver {

    // 1,000,000,000 x the largest numeric(19,4) quantity is 10^24 - 10^5, the most numeric(28,4) can hold
    const val MAX_SIZE_IN_ROOT = 1_000_000_000L

    const val CHAIN_HAS_CYCLE = "A unit's base unit chain cannot loop back to itself"
    const val CHAIN_BASE_UNIT_MISSING = "A unit's base unit does not exist"
    const val CHAIN_FACTOR_MISSING = "A unit with a base unit must have unitsOfBasePerUnit"
    const val CHAIN_SIZE_TOO_LARGE = "A unit cannot be larger than 1,000,000,000 of its group's root unit"

    fun resolvePlacements(nodes: Collection<UnitChainNode>): Map<UUID, UnitPlacement> {
        val nodeById = nodes.associateBy { it.id }
        val placementById = HashMap<UUID, UnitPlacement>(nodes.size)
        nodes.forEach { resolveChainOf(it, nodeById, placementById) }
        return placementById
    }

    private fun resolveChainOf(
        startNode: UnitChainNode,
        nodeById: Map<UUID, UnitChainNode>,
        placementById: MutableMap<UUID, UnitPlacement>
    ) {
        val unresolvedPath = ArrayList<UnitChainNode>()
        val idsOnPath = HashSet<UUID>()
        var current: UnitChainNode? = startNode
        while (current != null && current.id !in placementById) {
            if (!idsOnPath.add(current.id)) throw RtsGenericException(CHAIN_HAS_CYCLE)
            unresolvedPath.add(current)
            current = current.baseUnitId?.let { nodeById[it] ?: throw RtsGenericException(CHAIN_BASE_UNIT_MISSING) }
        }
        unresolvedPath.asReversed().forEach { node ->
            placementById[node.id] = placeOnTopOfBase(node, placementById)
        }
    }

    private fun placeOnTopOfBase(node: UnitChainNode, placementById: Map<UUID, UnitPlacement>): UnitPlacement {
        val baseUnitId = node.baseUnitId ?: return UnitPlacement(node.id, 1L)
        val unitsOfBasePerUnit = node.unitsOfBasePerUnit ?: throw RtsGenericException(CHAIN_FACTOR_MISSING)
        val basePlacement = placementById.getValue(baseUnitId)
        val sizeInRoot = try {
            Math.multiplyExact(unitsOfBasePerUnit, basePlacement.sizeInRoot)
        } catch (_: ArithmeticException) {
            throw RtsGenericException(CHAIN_SIZE_TOO_LARGE)
        }
        if (sizeInRoot > MAX_SIZE_IN_ROOT) throw RtsGenericException(CHAIN_SIZE_TOO_LARGE)
        return UnitPlacement(basePlacement.rootUnitId, sizeInRoot)
    }
}
