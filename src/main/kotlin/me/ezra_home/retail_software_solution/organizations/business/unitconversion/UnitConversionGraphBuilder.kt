package me.ezra_home.retail_software_solution.organizations.business.unitconversion

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.ConversionTargetDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraph
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.business.ConversionRatio
import org.springframework.stereotype.Component
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

        val edges = mutableMapOf<UUID, MutableMap<UUID, ConversionRatio>>()

        fun addEdge(fromId: UUID, toId: UUID, ratio: ConversionRatio) {
            edges.getOrPut(fromId) { mutableMapOf() }[toId] = ratio
            edges.getOrPut(toId) { mutableMapOf() }[fromId] = ratio.invert()
        }

        allUnits.filter { it.baseUnit != null && it.unitsOfBasePerUnit != null }.forEach { unit ->
            addEdge(
                unit.id,
                unit.baseUnit!!,
                ConversionRatio(unit.unitsOfBasePerUnit!!, 1L)
            )
        }

        manualConversions.forEach { conversion ->
            addEdge(
                conversion.fromUnitId,
                conversion.toUnitId,
                ConversionRatio(conversion.factorNumerator, conversion.factorDenominator)
            )
        }

        val map = allUnits.associate { startUnit ->
            val reachable = mutableMapOf<UUID, ConversionRatio>()
            val queue = ArrayDeque<Pair<UUID, ConversionRatio>>()
            queue.add(startUnit.id to ConversionRatio.IDENTITY)

            while (queue.isNotEmpty()) {
                val (currentId, currentRatio) = queue.removeFirst()
                edges[currentId]?.forEach { (neighbourId, edgeRatio) ->
                    if (neighbourId != startUnit.id && neighbourId !in reachable) {
                        val compounded = currentRatio.times(edgeRatio)
                        reachable[neighbourId] = compounded
                        queue.add(neighbourId to compounded)
                    }
                }
            }

            reachable[startUnit.id] = ConversionRatio.IDENTITY

            val conversions = reachable.mapNotNull { (toId, ratio) ->
                unitById[toId]?.let { toUnit ->
                    toId to ConversionTargetDto(toUnit.id, toUnit.name, toUnit.code, ratio.numerator, ratio.denominator)
                }
            }.toMap()

            startUnit.id to conversions
        }
        return UnitConversionGraph(map, allUnits.associate { it.id to it.name })
    }
}
