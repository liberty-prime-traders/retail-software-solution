package me.ezra_home.retail_software_solution.util.quantity

import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.math.RoundingMode

object BaseUnitDisplay {

    fun conversionFactor(storageUnitsPerUnit: Long, baseUnitSizeInRoot: Long): BigDecimal =
        Decimals.Scale10.divide(BigDecimal.valueOf(storageUnitsPerUnit), BigDecimal.valueOf(baseUnitSizeInRoot))

    fun baseQuantity(
        userProvidedQuantity: BigDecimal,
        storageUnitsPerUnit: Long,
        baseUnitSizeInRoot: Long,
        roundingMode: RoundingMode
    ): BigDecimal = ExactQuantity.ofUserProvided(userProvidedQuantity, storageUnitsPerUnit).inUnitOfSize(baseUnitSizeInRoot, roundingMode)

    fun storageQuantityInBaseUnit(storageQuantity: BigDecimal, baseUnitSizeInRoot: Long, roundingMode: RoundingMode): BigDecimal =
        ExactQuantity.ofStorage(storageQuantity).inUnitOfSize(baseUnitSizeInRoot, roundingMode)
}
