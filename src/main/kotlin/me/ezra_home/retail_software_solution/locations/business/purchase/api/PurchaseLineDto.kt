package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class PurchaseLineDto(
    val id: UUID,
    val purchaseId: UUID,
    val locationProductId: UUID,
    var unitCost: BigDecimal,
    val storageUnitsPerUnit: Long,
    val unitId: UUID,
    val storageQuantityExpected: ExactQuantity,
    val storageQuantityRemaining: ExactQuantity
)
