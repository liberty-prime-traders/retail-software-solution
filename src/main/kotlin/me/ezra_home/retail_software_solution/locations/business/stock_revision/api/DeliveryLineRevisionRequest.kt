package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity
import java.util.UUID

data class DeliveryLineRevisionRequest(
    val revisedDeliveryLineId: UUID,
    val userProvidedQuantityDelivered: UserProvidedQuantity,
    val unitId: UUID,
    val unitCost: Money,
    val reason: String,
)
