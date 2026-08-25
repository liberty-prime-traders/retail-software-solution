package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import java.time.OffsetDateTime
import java.util.UUID

data class ReceiptSnapshot(
    val id: UUID,
    val referenceNumber: String,
    val status: StockTransferReceiptStatus,
    val receivedById: UUID,
    val receivedAt: OffsetDateTime,
    val notes: String?,
    val confirmedDispatchLineRefs: Set<String>
)
