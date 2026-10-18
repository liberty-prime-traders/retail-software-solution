package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class StockTransferDraftLineDomainDto(
    val id: UUID,
    val referenceNumber: String,
    val stockTransferDispatchId: UUID,
    val locationProductId: UUID,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUnit: Long,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
