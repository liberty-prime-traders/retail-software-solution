package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.configuration.session.withLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockTransferReceiptStockUpdater
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedLineDto
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.InventoryEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass

@Service
class StockTransferDispatchedInventoryProcessor(
    private val stockTransferReceiptStockUpdater: StockTransferReceiptStockUpdater,
    private val stockTransferOrderService: StockTransferOrderService,
    private val locationService: LocationService,
    private val stockTransferSchemaGateway: StockTransferSchemaGateway
) : InventoryEventProcessor<StockTransferDispatchedEvent>, EventReissueHandler {

    override val eventType: KClass<StockTransferDispatchedEvent> = StockTransferDispatchedEvent::class

    // sourceDocumentId = transfer order ID (org schema) — session must be set to destination location schema by caller
    @TransactionalOnLocationSchema
    override fun reissue(sourceDocumentId: UUID) {
        val order = stockTransferOrderService.getById(sourceDocumentId)
        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)
        val destinationSchema = locationService.getSchemaByLocationId(order.destinationLocationId)

        val dispatchRecord = withLocationSchema(sourceSchema) {
            stockTransferSchemaGateway.readDispatchAndLines(order.referenceNumber)
        }

        val allLineRefs = dispatchRecord.lines.map { it.requiredReference() }
        val unprocessedRefs = stockTransferReceiptStockUpdater.findUnprocessedLineRefs(allLineRefs)
        if (unprocessedRefs.isEmpty()) return

        stockTransferReceiptStockUpdater.recordTransferReceipt(
            StockTransferDispatchedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = destinationSchema
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = order.id,
                orderReferenceNumber = order.referenceNumber,
                dispatchReferenceNumber = dispatchRecord.dispatch.requiredReference(),
                sourceLocationSchema = sourceSchema,
                destinationLocationSchema = destinationSchema,
                lines = dispatchRecord.lines
                    .filter { it.requiredReference() in unprocessedRefs }
                    .map { line ->
                        StockTransferDispatchedLineDto(
                            dispatchLineReferenceNumber = line.requiredReference(),
                            productId = line.productId,
                            quantityDispatched = line.quantityDispatched,
                            unitId = line.unitId,
                            baseUnitId = line.baseUnitId,
                            conversionFactor = line.conversionFactor,
                            unitCost = line.unitCost
                        )
                    }
            )
        )
    }

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
