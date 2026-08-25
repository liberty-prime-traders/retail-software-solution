package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import java.time.Instant
import java.util.UUID

data class StockTransferCancelledEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.LocationLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    override val sourceDocumentId: UUID,
    val orderReferenceNumber: String,
    val dispatchReferenceNumber: String,
    val sourceLocationSchema: String
) : TransactionEvent()
