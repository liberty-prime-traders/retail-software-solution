package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.updater.StockTransferStockUpdater
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
class StockTransferSchemaGateway(
    private val stockTransferDispatchRepository: StockTransferDispatchRepository,
    private val stockTransferDispatchLineRepository: StockTransferDispatchLineRepository,
    private val stockTransferReceiptRepository: StockTransferReceiptRepository,
    private val stockTransferReceiptLineRepository: StockTransferReceiptLineRepository,
    private val reconciledTransferLineFetcher: ReconciledTransferLineFetcher,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockTransferStockUpdater: StockTransferStockUpdater,
) {

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun readBaseUnitIdsByOrgProductId(orgProductIds: Collection<UUID>): Map<UUID, UUID> =
        locationProductDataFetcher.getBaseUnitIdsByOrgProductIds(orgProductIds)


    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun readDispatchLine(dispatchLineReferenceNumber: String): StockTransferDispatchLineEntity =
        stockTransferDispatchLineRepository.findByReferenceNumber(dispatchLineReferenceNumber)
            ?: throw RtsGenericException("Dispatch line $dispatchLineReferenceNumber not found")

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun readDispatchAndLines(orderReferenceNumber: String): DispatchWithLines {
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(orderReferenceNumber)
            ?: throw RtsGenericException("Dispatch not found for order $orderReferenceNumber")
        val lines = stockTransferDispatchLineRepository.findByStockTransferDispatchId(dispatch.id!!)
        return DispatchWithLines(dispatch, lines)
    }

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun readReceiptSnapshot(orderReferenceNumber: String): ReceiptSnapshot? {
        val receipt = stockTransferReceiptRepository.findByStockTransferOrderRef(orderReferenceNumber) ?: return null
        val confirmedDispatchLineReferenceNumbers = stockTransferReceiptLineRepository
            .findByStockTransferReceiptId(receipt.id!!)
            .map { it.stockTransferDispatchLineRef }
            .toSet()
        return ReceiptSnapshot(
            id = receipt.id!!,
            referenceNumber = receipt.requiredReference(),
            status = receipt.status,
            receivedById = receipt.receivedById,
            receivedAt = receipt.receivedAt,
            notes = receipt.notes,
            confirmedDispatchLineReferenceNumbers = confirmedDispatchLineReferenceNumbers
        )
    }

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun buildDispatchWithLines(orderReferenceNumber: String, isDraft: Boolean, confirmedDispatchLineReferenceNumbers: Set<String>): DispatchWithReconciledLines {
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(orderReferenceNumber)
            ?: throw RtsGenericException("Dispatch not found for order $orderReferenceNumber")
        val lines = if (isDraft) {
            reconciledTransferLineFetcher.draftLines(dispatch.id!!)
        } else {
            reconciledTransferLineFetcher.dispatchLines(dispatch.id!!, confirmedDispatchLineReferenceNumbers)
        }
        return DispatchWithReconciledLines(dispatch = dispatch.toDomainDto(), lines = lines)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun markDispatchCompleted(orderReferenceNumber: String) {
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(orderReferenceNumber) ?: return
        dispatch.status = StockTransferStatus.COMPLETED
        stockTransferDispatchRepository.save(dispatch)
    }

    // The restore and the status share one transaction, so a blocked restore leaves the dispatch DISPATCHED
    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun cancelDispatch(orderReferenceNumber: String): String {
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(orderReferenceNumber)
            ?: throw RtsGenericException("Dispatch not found for order $orderReferenceNumber")
        val dispatchLineReferences = stockTransferDispatchLineRepository
            .findByStockTransferDispatchId(dispatch.id!!)
            .map { it.requiredReference() }
        stockTransferStockUpdater.restoreStockForLines(dispatch.requiredReference(), dispatchLineReferences)
        dispatch.status = StockTransferStatus.CANCELLED
        stockTransferDispatchRepository.save(dispatch)
        return dispatch.requiredReference()
    }
}

data class DispatchWithLines(
    val dispatch: StockTransferDispatchEntity,
    val lines: List<StockTransferDispatchLineEntity>
)

data class DispatchWithReconciledLines(
    val dispatch: StockTransferDispatchDomainDto,
    val lines: List<ReconciledTransferLine>
)
