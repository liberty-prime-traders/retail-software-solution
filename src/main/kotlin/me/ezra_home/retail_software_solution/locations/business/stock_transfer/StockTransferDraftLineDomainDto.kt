package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.util.business.ConversionRatio
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class StockTransferDraftLineDomainDto(
    val id: UUID,
    val referenceNumber: String,
    val stockTransferDispatchId: UUID,
    val locationProductId: UUID,
    val quantity: BigDecimal,
    val unitId: UUID,
    val conversionRatio: ConversionRatio,
    val baseUnitId: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
