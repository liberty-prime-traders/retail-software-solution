package me.ezra_home.retail_software_solution.locations.business.purchase.api

import java.math.BigDecimal
import java.util.UUID

data class PurchaseLineDto(
    val id: UUID,
    val purchaseId: UUID,
    val locationProductId: UUID,
    var quantityOrdered: BigDecimal,
    var unitCost: BigDecimal,
    var quantityDelivered: BigDecimal,
    var quantityCanceled: BigDecimal
)
