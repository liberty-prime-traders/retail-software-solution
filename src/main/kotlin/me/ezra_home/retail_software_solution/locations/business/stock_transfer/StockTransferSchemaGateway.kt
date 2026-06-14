package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher

import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
class StockTransferSchemaGateway(
    private val stockTransferDispatchRepository: StockTransferDispatchRepository,
    private val stockTransferDraftLineRepository: StockTransferDraftLineRepository,
    private val stockTransferDispatchLineRepository: StockTransferDispatchLineRepository,
    private val stockTransferReceiptRepository: StockTransferReceiptRepository,
    private val stockTransferReceiptLineRepository: StockTransferReceiptLineRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
) {

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun readDispatchLine(dispatchLineRef: String): StockTransferDispatchLineEntity =
        stockTransferDispatchLineRepository.findByReferenceNumber(dispatchLineRef)
            ?: throw RtsGenericException("Dispatch line $dispatchLineRef not found")

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun readAllDispatchLines(orderRef: String): List<StockTransferDispatchLineEntity> {
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(orderRef)
            ?: throw RtsGenericException("Dispatch not found for order $orderRef")
        return stockTransferDispatchLineRepository.findByStockTransferDispatchId(dispatch.id!!)
    }

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun readDispatchAndLines(orderRef: String): DispatchWithLines {
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(orderRef)
            ?: throw RtsGenericException("Dispatch not found for order $orderRef")
        val lines = stockTransferDispatchLineRepository.findByStockTransferDispatchId(dispatch.id!!)
        return DispatchWithLines(dispatch, lines)
    }

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun readReceiptSnapshot(orderRef: String): ReceiptSnapshot? {
        val receipt = stockTransferReceiptRepository.findByStockTransferOrderRef(orderRef) ?: return null
        val confirmedRefs = stockTransferReceiptLineRepository
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
            confirmedDispatchLineRefs = confirmedRefs
        )
    }

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun buildDispatchWithLines(orderRef: String, isDraft: Boolean, confirmedRefs: Set<String>): DispatchWithReconciledLines {
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(orderRef)
            ?: throw RtsGenericException("Dispatch not found for order $orderRef")
        val lines = if (isDraft) draftLines(dispatch.id!!) else dispatchLines(dispatch.id!!, confirmedRefs)
        return DispatchWithReconciledLines(dispatch = dispatch.toDomainDto(), lines = lines)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun markDispatchCompleted(orderRef: String) {
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(orderRef) ?: return
        dispatch.status = StockTransferStatus.COMPLETED
        stockTransferDispatchRepository.save(dispatch)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun cancelDispatch(orderRef: String): String {
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(orderRef)
            ?: throw RtsGenericException("Dispatch not found for order $orderRef")
        dispatch.status = StockTransferStatus.CANCELLED
        stockTransferDispatchRepository.save(dispatch)
        return dispatch.requiredReference()
    }

    private fun draftLines(dispatchId: UUID): List<ReconciledTransferLine> {
        val lines = stockTransferDraftLineRepository.findByStockTransferDispatchId(dispatchId)
        val labels = locationProductDataFetcher.findSummaryByIds(lines.map { it.locationProductId }.toSet())
        return lines.map { line ->
            ReconciledTransferLine(
                dispatchLineRef = line.requiredReference(),
                productLabel = labels.getValue(line.locationProductId).label,
                quantity = line.quantity,
                unitId = line.unitId,
                baseUnitId = line.baseUnitId,
                conversionFactor = line.conversionFactor,
                unitCost = null,
                quantityReceived = null
            )
        }
    }

    private fun dispatchLines(dispatchId: UUID, confirmedRefs: Set<String>): List<ReconciledTransferLine> {
        val lines = stockTransferDispatchLineRepository.findByStockTransferDispatchId(dispatchId)
        val labels = locationProductDataFetcher.findSummaryByIds(lines.map { it.locationProductId }.toSet())
        return lines.map { line ->
            ReconciledTransferLine(
                dispatchLineRef = line.requiredReference(),
                productLabel = labels.getValue(line.locationProductId).label,
                quantity = line.quantityDispatched,
                unitId = line.unitId,
                baseUnitId = line.baseUnitId,
                conversionFactor = line.conversionFactor,
                unitCost = line.unitCost,
                quantityReceived = if (line.requiredReference() in confirmedRefs) line.quantityDispatched else null
            )
        }
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
