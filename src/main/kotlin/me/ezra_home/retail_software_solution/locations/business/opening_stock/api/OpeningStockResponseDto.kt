package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class OpeningStockResponseDto(
    val referenceNumber: String,
    val locationProductId: UUID,
    val productLabel: String,
    val quantity: BigDecimal,
    val unitCost: BigDecimal,
    val declaredBy: String,
    val declaredAt: OffsetDateTime
)
