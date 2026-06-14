package me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api

import java.util.UUID

data class StockTransferOrderResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val sourceLocationId: UUID,
    val destinationLocationId: UUID,
    val status: StockTransferStatus,
    val notes: String?
)

fun StockTransferOrderDomainDto.toResponseDto() = StockTransferOrderResponseDto(
    id = id,
    referenceNumber = referenceNumber,
    sourceLocationId = sourceLocationId,
    destinationLocationId = destinationLocationId,
    status = status,
    notes = notes
)
