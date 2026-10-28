package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.util.business.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class OpeningStockDeclaredEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.LocationLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    val openingStockId: UUID,
    val ledgerSourceReferenceNumber: String,
    val locationProductId: UUID,
    val totalCost: Money,
    val postingDate: LocalDate
) : TransactionEvent() {
    override val sourceDocumentId: UUID = openingStockId
}
