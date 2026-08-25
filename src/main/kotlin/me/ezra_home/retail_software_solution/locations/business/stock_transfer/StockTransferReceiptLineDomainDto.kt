package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class StockTransferReceiptLineDomainDto(
    val id: UUID,
    val referenceNumber: String,
    val stockTransferReceiptId: UUID,
    val stockTransferDispatchLineRef: String,
    val locationProductId: UUID,
    val quantityReceived: BigDecimal,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
