package me.ezra_home.retail_software_solution.util.quantity

import me.ezra_home.retail_software_solution.util.business.ConversionRatio
import java.math.BigDecimal
import java.math.RoundingMode

object BaseUnitDisplay {

    fun conversionFactor(storageUnitsPerUserProvidedUnit: Long, storageUnitsPerBaseUnit: Long): BigDecimal =
        ConversionRatio(storageUnitsPerUserProvidedUnit, storageUnitsPerBaseUnit).factor()

    fun baseQuantity(
        userProvidedQuantity: UserProvidedQuantity,
        storageUnitsPerUserProvidedUnit: Long,
        storageUnitsPerBaseUnit: Long,
        roundingMode: RoundingMode
    ): BigDecimal = ExactQuantity.ofUserProvided(userProvidedQuantity, storageUnitsPerUserProvidedUnit).toDisplayQuantity(storageUnitsPerBaseUnit, roundingMode)

    fun storageQuantityInBaseUnit(storageQuantity: BigDecimal, storageUnitsPerBaseUnit: Long, roundingMode: RoundingMode): BigDecimal =
        ExactQuantity.ofStorage(storageQuantity).toDisplayQuantity(storageUnitsPerBaseUnit, roundingMode)
}
