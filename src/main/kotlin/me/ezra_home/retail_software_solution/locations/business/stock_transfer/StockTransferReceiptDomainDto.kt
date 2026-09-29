package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import java.time.OffsetDateTime
import java.util.UUID

data class StockTransferReceiptDomainDto(
    val id: UUID,
    val referenceNumber: String,
    val stockTransferOrderRef: String,
    val receivedById: UUID,
    val receivedAt: OffsetDateTime,
    val status: StockTransferReceiptStatus,
    val notes: String?,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
