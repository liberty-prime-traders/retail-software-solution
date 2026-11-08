package me.ezra_home.retail_software_solution.locations.business.delivery.api

import java.math.BigDecimal
import java.util.UUID

data class ReplacementDeliveryLineDto(
    val deliveryReferenceNumber: String,
    val revisedLineReferenceNumber: String,
    val replacementLineReferenceNumber: String,
    val purchaseLineId: UUID,
    val userProvidedQuantityDelivered: BigDecimal,
    val unitCost: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
)
