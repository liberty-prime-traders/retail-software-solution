package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockTransferStockUpdater
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferCancelledEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.InventoryEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.reflect.KClass

@Service
class StockTransferCancelledInventoryProcessor(
    private val stockTransferDispatchLineRepository: StockTransferDispatchLineRepository,
    private val stockTransferDispatchRepository: StockTransferDispatchRepository,
    private val stockTransferStockUpdater: StockTransferStockUpdater,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher
) : InventoryEventProcessor<StockTransferCancelledEvent>, EventReissueHandler {

    private val log = LoggerFactory.getLogger(StockTransferCancelledInventoryProcessor::class.java)

    override val eventType: KClass<StockTransferCancelledEvent> = StockTransferCancelledEvent::class

    // sourceDocumentId = transfer order ID (org schema) — session must be set to source location schema by caller
    @TransactionalOnLocationSchema
    override fun reissue(sourceDocumentId: UUID) {
        val order = stockTransferOrderDataFetcher.getById(sourceDocumentId)
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(order.referenceNumber)
            ?: run {
                log.warn("Skipping reissue for cancelled transfer: no dispatch for order ${order.referenceNumber}")
                return
            }
        stockTransferStockUpdater.restoreStockForLines(findUnrestoredRefs(dispatch.requiredReference()))
    }

    @TransactionalOnLocationSchema(readOnly = true)
    override fun shouldProcess(event: StockTransferCancelledEvent): Boolean =
        stockTransferDispatchRepository.findByReferenceNumber(event.dispatchReferenceNumber) != null

    @TransactionalOnLocationSchema
    override fun handle(event: StockTransferCancelledEvent) {
        stockTransferStockUpdater.restoreStockForLines(findUnrestoredRefs(event.dispatchReferenceNumber))
    }

    private fun findUnrestoredRefs(dispatchReferenceNumber: String): Set<String> {
        val dispatch = stockTransferDispatchRepository.findByReferenceNumber(dispatchReferenceNumber)
        if (dispatch == null) {
            log.warn("Skipping StockTransferCancelledEvent: dispatch $dispatchReferenceNumber not found")
            return emptySet()
        }
        val lineRefs = stockTransferDispatchLineRepository
            .findByStockTransferDispatchId(dispatch.id!!)
            .map { it.requiredReference() }
        return stockTransferStockUpdater.findUnrestoredLineRefs(lineRefs)
    }
}
