package me.ezra_home.retail_software_solution.util.quantity

import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class BreakdownUnit(
    val id: UUID,
    val name: String,
    val sizeInRoot: Long,
    val rootUnitId: UUID,
    val createdOn: OffsetDateTime
) {
    val isRoot: Boolean = id == rootUnitId
}

// One plan per product base unit, built from the organization's units fetched once per request
class QuantityBreakdown private constructor(
    private val baseUnitName: String,
    private val wholeUnitLevels: List<BreakdownUnit>,
    private val rootUnit: BreakdownUnit,
    private val unitNamesById: Map<UUID, String>
) {

    // As typed, in the line's own unit and never broken down: "2.5 Bales"
    fun renderUserProvided(userProvidedQuantity: BigDecimal, unitId: UUID): String =
        describe(Decimals.stripZeroesAndRound(userProvidedQuantity), userProvidedQuantity, unitNamesById.getValue(unitId))

    fun render(storageQuantity: ExactQuantity): String {
        val signedQuantity = storageQuantity.toStorageQuantity()
        val isNegative = signedQuantity.signum() < 0
        val breakdownText = renderMagnitude(signedQuantity.abs())
        return if (isNegative && breakdownText != ZERO_TEXT) "-$breakdownText" else breakdownText
    }

    private fun renderMagnitude(magnitude: BigDecimal): String {
        var remaining = magnitude
        val parts = mutableListOf<String>()
        wholeUnitLevels.forEach { level ->
            val wholeCount = remaining.divideToIntegralValue(BigDecimal.valueOf(level.sizeInRoot))
            if (wholeCount.signum() > 0) {
                parts.add(describe(wholeCount.toBigInteger().toString(), wholeCount, level.name))
                remaining = remaining.subtract(wholeCount.multiply(BigDecimal.valueOf(level.sizeInRoot)))
            }
        }
        if (remaining.signum() > 0) {
            parts.add(describe(Decimals.stripZeroesAndRound(remaining), remaining, rootUnit.name))
        }
        return if (parts.isEmpty()) "$ZERO_TEXT ${DisplayFormatters.pluralize(baseUnitName, BigDecimal.ZERO)}"
        else parts.joinToString(" ")
    }

    private fun describe(countText: String, count: BigDecimal, unitName: String): String =
        "$countText ${DisplayFormatters.pluralize(unitName, count)}"

    companion object {
        private const val ZERO_TEXT = "0"

        // Equal sizes are taken in createdOn, then id order, so the first takes the quantity and the rest render nothing
        fun forBaseUnit(allUnits: Collection<BreakdownUnit>, baseUnitId: UUID): QuantityBreakdown {
            val baseUnit = allUnits.first { it.id == baseUnitId }
            val unitsOfRoot = allUnits.filter { it.rootUnitId == baseUnit.rootUnitId }
            val rootUnit = unitsOfRoot.first { it.isRoot }
            val wholeUnitLevels = unitsOfRoot
                .filter { !it.isRoot && it.sizeInRoot > rootUnit.sizeInRoot }
                .filter { it.sizeInRoot < baseUnit.sizeInRoot || it.id == baseUnit.id }
                .sortedWith(compareByDescending<BreakdownUnit> { it.sizeInRoot }.thenBy { it.createdOn }.thenBy { it.id })
            return QuantityBreakdown(
                baseUnit.name, wholeUnitLevels, rootUnit,
                unitsOfRoot.associate { it.id to it.name }
            )
        }
    }
}
