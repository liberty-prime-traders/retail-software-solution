package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.locations.business.stock_transfer.ReconciledTransferLine
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferPerspective
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptStatus
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferSummaryDto
import java.time.OffsetDateTime
import java.util.UUID

data class StockTransferResponse(
    val summary: StockTransferSummaryDto,
    val dispatch: StockTransferDispatchResponseDto,
    val receipt: StockTransferReceiptResponseDto?,
    val perspective: StockTransferPerspective
)

data class StockTransferDispatchResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val status: StockTransferStatus,
    val dispatchedById: UUID?,
    val dispatchedAt: OffsetDateTime?,
    val notes: String?,
    val lines: List<ReconciledTransferLine>
)

data class StockTransferReceiptResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val status: StockTransferReceiptStatus,
    val receivedById: UUID,
    val receivedAt: OffsetDateTime,
    val notes: String?,
    val lines: List<ReconciledTransferLine>
)

