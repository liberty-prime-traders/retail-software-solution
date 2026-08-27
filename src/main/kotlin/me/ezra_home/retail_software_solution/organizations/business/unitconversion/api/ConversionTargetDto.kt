package me.ezra_home.retail_software_solution.organizations.business.unitconversion.api

import me.ezra_home.retail_software_solution.util.business.ConversionRatio
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.util.UUID

data class ConversionTargetDto(
    val id: UUID,
    val name: String,
    val code: String,
    val numerator: Long,
    val denominator: Long,
    val factor: BigDecimal = Decimals.divideScale4(numerator.toBigDecimal(), denominator.toBigDecimal())
) {

    fun applyTo(quantity: BigDecimal): BigDecimal = Decimals.divideScale4(
        quantity.multiply(numerator.toBigDecimal()),
        denominator.toBigDecimal()
    )
}

typealias ConversionTargets = Map<UUID, ConversionTargetDto>

data class UnitConversionGraph(
    private val fromUnitToTargets: Map<UUID, ConversionTargets>,
    private val unitNamesById: Map<UUID, String> = emptyMap()
) {

    fun getFullGraph(): Map<UUID, ConversionTargets> = fromUnitToTargets

    fun getTarget(fromUnitId: UUID, toUnitId: UUID): ConversionTargetDto {
        val fromName = label(fromUnitId)
        val targets = fromUnitToTargets[fromUnitId]
            ?: throw RtsGenericException("No conversion targets found for unit $fromName")
        return targets[toUnitId]
            ?: throw RtsGenericException("No conversion target found from $fromName to ${label(toUnitId)}")
    }

    fun getFactor(fromUnitId: UUID, toUnitId: UUID): BigDecimal {
        if (fromUnitId == toUnitId) return BigDecimal.ONE
        return getTarget(fromUnitId, toUnitId).factor
    }

    fun getRatio(fromUnitId: UUID, toUnitId: UUID): ConversionRatio {
        if (fromUnitId == toUnitId) return ConversionRatio.IDENTITY
        val target = getTarget(fromUnitId, toUnitId)
        return ConversionRatio(target.numerator, target.denominator)
    }

    private fun label(unitId: UUID): String =
        unitNamesById[unitId] ?: throw RtsGenericException("Unit name not found for id $unitId")
}
