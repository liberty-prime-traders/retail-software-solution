package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import java.time.OffsetDateTime
import java.util.UUID

data class StockTransferDispatchDomainDto(
    val id: UUID,
    val referenceNumber: String,
    val stockTransferOrderRef: String,
    val status: StockTransferStatus,
    val dispatchedById: UUID?,
    val dispatchedAt: OffsetDateTime?,
    val notes: String?,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
