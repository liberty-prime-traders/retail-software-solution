package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockTransferStockUpdater
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferCancelledEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.InventoryEventProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class StockTransferCancelledInventoryProcessor(
    private val stockTransferDispatchLineRepository: StockTransferDispatchLineRepository,
    private val stockTransferDispatchRepository: StockTransferDispatchRepository,
    private val stockTransferStockUpdater: StockTransferStockUpdater
) : InventoryEventProcessor<StockTransferCancelledEvent> {

    private val log = LoggerFactory.getLogger(StockTransferCancelledInventoryProcessor::class.java)

    override val eventType: KClass<StockTransferCancelledEvent> = StockTransferCancelledEvent::class

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
