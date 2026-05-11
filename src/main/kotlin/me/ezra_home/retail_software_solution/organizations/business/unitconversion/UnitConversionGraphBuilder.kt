package me.ezra_home.retail_software_solution.organizations.business.unitconversion

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.ConversionTargetDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraph
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class UnitConversionGraphBuilder(
    private val unitValueFetcher: UnitValueFetcher,
    private val unitConversionRepository: UnitConversionRepository
) {

    private val cache = ConcurrentHashMap<String, UnitConversionGraph>()

    fun getOrLoad(): UnitConversionGraph {
        val orgSchema = SessionContextProvider.getOrganizationSchema()
        return cache.getOrPut(orgSchema) { buildGraph() }
    }

    fun invalidate() {
        val orgSchema = SessionContextProvider.getOrganizationSchema()
        cache.remove(orgSchema)
    }

    private fun buildGraph(): UnitConversionGraph {
        val allUnits = unitValueFetcher.getAllUnitValues().toList()
        val unitById = allUnits.associateBy { it.id }
        val manualConversions = unitConversionRepository.findAll()

        val edges = mutableMapOf<UUID, MutableMap<UUID, Pair<Long, Long>>>()

        fun addEdge(fromId: UUID, toId: UUID, numerator: Long, denominator: Long) {
            edges.getOrPut(fromId) { mutableMapOf() }[toId] = numerator to denominator
            edges.getOrPut(toId) { mutableMapOf() }[fromId] = denominator to numerator
        }

        allUnits.filter { it.baseUnit != null && it.conversionFactor != null }.forEach { unit ->
            val (n, d) = unit.conversionFactor!!.toRationalPair()
            addEdge(unit.id, unit.baseUnit!!, n, d)
        }

        manualConversions.forEach { conversion ->
            val (n, d) = conversion.factor.toRationalPair()
            addEdge(conversion.fromUnitId, conversion.toUnitId, n, d)
        }

        val map = allUnits.associate { startUnit ->
            val reachable = mutableMapOf<UUID, Pair<Long, Long>>()
            val queue = ArrayDeque<RationalNode>()
            queue.add(RationalNode(startUnit.id, 1L, 1L))

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                edges[current.id]?.forEach { (neighbourId, edge) ->
                    if (neighbourId != startUnit.id && neighbourId !in reachable) {
                        val (compN, compD) = compound(current.numerator, current.denominator, edge.first, edge.second)
                        reachable[neighbourId] = compN to compD
                        queue.add(RationalNode(neighbourId, compN, compD))
                    }
                }
            }

            reachable[startUnit.id] = 1L to 1L

            val conversions = reachable.mapNotNull { (toId, pair) ->
                unitById[toId]?.let { toUnit ->
                    toId to ConversionTargetDto(toUnit.id, toUnit.name, toUnit.code, pair.first, pair.second)
                }
            }.toMap()

            startUnit.id to conversions
        }
        return UnitConversionGraph(map, allUnits.associate { it.id to it.name })
    }
}

private data class RationalNode(val id: UUID, val numerator: Long, val denominator: Long)

private fun BigDecimal.toRationalPair(): Pair<Long, Long> {
    val scaled = this.multiply(BigDecimal(100)).toLong()
    val g = gcd(scaled, 100L)
    return (scaled / g) to (100L / g)
}

private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

private fun compound(aN: Long, aD: Long, bN: Long, bD: Long): Pair<Long, Long> {
    val n = aN * bN
    val d = aD * bD
    val g = gcd(n, d)
    return (n / g) to (d / g)
}
