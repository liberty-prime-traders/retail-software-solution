package me.ezra_home.retail_software_solution.organizations.business.unitconversion

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.ConversionTargetDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraph
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitChainNode
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitChainResolver
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import me.ezra_home.retail_software_solution.util.business.ConversionRatio
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.ConcurrentHashMap

@Component
class UnitConversionGraphBuilder(
    private val unitValueFetcher: UnitValueFetcher,
    private val unitGroupDataFetcher: UnitGroupDataFetcher
) {

    private val cache = ConcurrentHashMap<String, UnitConversionGraph>()

    fun getOrLoad(): UnitConversionGraph {
        val orgSchema = SessionContextProvider.getOrganizationSchema()
        return cache.getOrPut(orgSchema) { buildGraph() }
    }

    // Evicts immediately so reads in the writing transaction see their own changes,
    // and again after completion: a concurrent request can rebuild from the old committed rows in between,
    // and a rolled-back transaction would cache rows that never existed
    fun invalidate() {
        val orgSchema = SessionContextProvider.getOrganizationSchema()
        cache.remove(orgSchema)
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    cache.remove(orgSchema)
                }
            })
        }
    }

    private fun buildGraph(): UnitConversionGraph {
        val allUnits = unitValueFetcher.getAllUnitValues().toList()
        val placementByUnitId = UnitChainResolver.resolvePlacements(
            allUnits.map { UnitChainNode(it.id, it.baseUnit, it.unitsOfBasePerUnit) }
        )
        val targetsByUnitId = allUnits.groupBy { it.unitGroupId }.values.flatMap { unitsOfGroup ->
            unitsOfGroup.map { fromUnit ->
                val fromSizeInRoot = placementByUnitId.getValue(fromUnit.id).sizeInRoot
                // Miscellaneous units stand alone: sharing a group does not make them convertible to each other
                val reachableUnits = if (unitGroupDataFetcher.isMiscellaneous(fromUnit.unitGroupId)) listOf(fromUnit) else unitsOfGroup
                fromUnit.id to reachableUnits.associate { toUnit ->
                    toUnit.id to conversionTarget(toUnit, fromSizeInRoot, placementByUnitId.getValue(toUnit.id).sizeInRoot)
                }
            }
        }.toMap()
        val breakdownUnits = allUnits.map { unit ->
            val placement = placementByUnitId.getValue(unit.id)
            BreakdownUnit(unit.id, unit.name, placement.sizeInRoot, placement.rootUnitId, unit.createdOn)
        }
        return UnitConversionGraph(
            targetsByUnitId,
            allUnits.associate { it.id to it.name },
            placementByUnitId,
            breakdownUnits
        )
    }

    private fun conversionTarget(
        toUnit: UnitValueResponseDto,
        fromSizeInRoot: Long,
        toSizeInRoot: Long
    ): ConversionTargetDto {
        val ratio = ConversionRatio(fromSizeInRoot, toSizeInRoot).reduced()
        return ratio.buildFromParts { numerator, denominator -> ConversionTargetDto(toUnit.id, toUnit.name, toUnit.code, numerator, denominator) }
    }
}
