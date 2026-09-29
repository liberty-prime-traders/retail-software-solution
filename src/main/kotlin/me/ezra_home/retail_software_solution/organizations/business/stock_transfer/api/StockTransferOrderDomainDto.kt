package me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class StockTransferOrderDomainDto(
    val id: UUID,
    val referenceNumber: String,
    val sourceLocationId: UUID,
    val destinationLocationId: UUID,
    val status: StockTransferStatus,
    val notes: String?,
    val lineCount: Int?,
    val totalDispatchedCost: BigDecimal?,
    val dispatchedAt: OffsetDateTime?,
    val dispatchedByName: String?,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
