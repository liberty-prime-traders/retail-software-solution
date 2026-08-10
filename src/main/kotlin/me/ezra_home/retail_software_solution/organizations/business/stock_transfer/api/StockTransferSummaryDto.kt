package me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api

import java.math.BigDecimal
import java.time.OffsetDateTime

data class StockTransferSummaryDto(
    val referenceNumber: String,
    val sourceLocationName: String?,
    val destinationLocationName: String?,
    val status: StockTransferStatus,
    val lineCount: Int?,
    val totalDispatchedCost: BigDecimal?,
    val dispatchedAt: OffsetDateTime?,
    val dispatchedBy: String?
)

fun StockTransferOrderDomainDto.toSummaryDto(sourceLocationName: String?, destinationLocationName: String?) =
    StockTransferSummaryDto(
        referenceNumber = referenceNumber,
        sourceLocationName = sourceLocationName,
        destinationLocationName = destinationLocationName,
        status = status,
        lineCount = lineCount,
        totalDispatchedCost = totalDispatchedCost,
        dispatchedAt = dispatchedAt,
        dispatchedBy = dispatchedByName
    )
