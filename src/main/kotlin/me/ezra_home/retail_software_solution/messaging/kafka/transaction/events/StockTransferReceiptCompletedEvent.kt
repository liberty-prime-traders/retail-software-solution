package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.util.business.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class StockTransferReceiptCompletedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.LocationLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    override val sourceDocumentId: UUID,
    val orderReferenceNumber: String,
    val dispatchReferenceNumber: String,
    val totalCost: Money,
    val receiptReferenceNumber: String,
    val postingDate: LocalDate
) : TransactionEvent()
