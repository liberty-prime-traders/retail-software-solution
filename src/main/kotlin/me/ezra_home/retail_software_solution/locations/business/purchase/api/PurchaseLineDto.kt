package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.util.business.ConversionRatio
import java.math.BigDecimal
import java.util.UUID

data class PurchaseLineDto(
    val id: UUID,
    val purchaseId: UUID,
    val locationProductId: UUID,
    var unitCost: BigDecimal,
    val conversionRatio: ConversionRatio,
    val unitId: UUID,
    val expectedQuantity: BigDecimal,
    val remainingQuantity: BigDecimal
)
