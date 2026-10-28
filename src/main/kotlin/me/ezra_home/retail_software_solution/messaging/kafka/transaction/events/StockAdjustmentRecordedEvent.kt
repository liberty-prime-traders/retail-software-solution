package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.util.business.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class StockAdjustmentRecordedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.LocationLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    val stockAdjustmentId: UUID,
    val ledgerSourceReferenceNumber: String,
    val debitAccountCode: String,
    val creditAccountCode: String,
    val totalCost: Money,
    val postingDate: LocalDate
) : TransactionEvent() {
    override val sourceDocumentId: UUID = stockAdjustmentId
}
