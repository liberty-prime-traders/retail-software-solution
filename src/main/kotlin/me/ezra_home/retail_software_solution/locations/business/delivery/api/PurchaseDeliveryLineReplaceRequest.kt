package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity
import java.util.UUID

data class PurchaseDeliveryLineReplaceRequest(
    val revisedDeliveryLineId: UUID,
    val userProvidedQuantityDelivered: UserProvidedQuantity,
    val unitId: UUID,
    val unitCost: Money,
)
