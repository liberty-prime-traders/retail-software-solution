package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

data class LastPurchaseCostUpdateDto(
    val locationProductId: UUID,
    val unitCost: BigDecimal,
    val storageUnitsPerUserProvidedUnit: Long,
) {

    fun costPerBaseUnit(storageUnitsPerBaseUnit: Long): BigDecimal =
        Decimals.Scale4.divide(
            unitCost.multiply(BigDecimal.valueOf(storageUnitsPerBaseUnit)),
            BigDecimal.valueOf(storageUnitsPerUserProvidedUnit),
        )
}
