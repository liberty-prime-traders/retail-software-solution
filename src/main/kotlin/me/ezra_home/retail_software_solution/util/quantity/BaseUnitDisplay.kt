package me.ezra_home.retail_software_solution.util.quantity

import java.math.BigDecimal
import java.math.RoundingMode

// Display only: each figure divides once from exact values, never from an already-rounded factor
object BaseUnitDisplay {

    fun conversionFactor(storageUnitsPerUnit: Long, baseUnitSizeInRoot: Long): BigDecimal =
        BigDecimal.valueOf(storageUnitsPerUnit).divide(BigDecimal.valueOf(baseUnitSizeInRoot), ExactQuantity.DISPLAY_SCALE, RoundingMode.HALF_UP)

    fun baseQuantity(
        userProvidedQuantity: BigDecimal,
        storageUnitsPerUnit: Long,
        baseUnitSizeInRoot: Long,
        roundingMode: RoundingMode
    ): BigDecimal = ExactQuantity.ofUserProvided(userProvidedQuantity, storageUnitsPerUnit).inUnitOfSize(baseUnitSizeInRoot, roundingMode)

    fun storageQuantityInBaseUnit(storageQuantity: BigDecimal, baseUnitSizeInRoot: Long, roundingMode: RoundingMode): BigDecimal =
        ExactQuantity.ofStorage(storageQuantity).inUnitOfSize(baseUnitSizeInRoot, roundingMode)
}
