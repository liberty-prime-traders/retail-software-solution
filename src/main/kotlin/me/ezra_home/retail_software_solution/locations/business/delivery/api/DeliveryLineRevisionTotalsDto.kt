package me.ezra_home.retail_software_solution.locations.business.delivery.api

import java.math.BigDecimal
import java.util.UUID

data class DeliveryLineRevisionTotalsDto(
    val purchaseId: UUID,
    val revisedLineTotal: BigDecimal,
    val replacementLineTotal: BigDecimal,
)
