package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockTransferReceiptStockUpdater
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.InventoryEventProcessor
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class StockTransferDispatchedInventoryProcessor(
    private val stockTransferReceiptStockUpdater: StockTransferReceiptStockUpdater
) : InventoryEventProcessor<StockTransferDispatchedEvent> {

    override val eventType: KClass<StockTransferDispatchedEvent> = StockTransferDispatchedEvent::class

    @TransactionalOnLocationSchema(readOnly = true)
    override fun shouldProcess(event: StockTransferDispatchedEvent): Boolean {
        return findUnprocessedDispatchLineRefs(event).isNotEmpty()
    }

    @TransactionalOnLocationSchema
    override fun handle(event: StockTransferDispatchedEvent) {
        val unprocessedRefs = findUnprocessedDispatchLineRefs(event)
        if (unprocessedRefs.isEmpty()) return
        val linesToProcess = event.lines.filter { it.dispatchLineReferenceNumber in unprocessedRefs }
        stockTransferReceiptStockUpdater.recordTransferReceipt(event.copy(lines = linesToProcess))
    }

    private fun findUnprocessedDispatchLineRefs(event: StockTransferDispatchedEvent): Set<String> {
        val lineRefs = event.lines.map { it.dispatchLineReferenceNumber }
        return stockTransferReceiptStockUpdater.findUnprocessedLineRefs(lineRefs)
    }
}
