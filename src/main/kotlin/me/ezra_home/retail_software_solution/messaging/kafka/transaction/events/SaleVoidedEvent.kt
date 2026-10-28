package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.util.business.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class SaleVoidedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.LocationLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    override val sourceDocumentId: UUID,
    val contactId: UUID,
    val saleReferenceNumber: String,
    val taxableAmount: Money,
    val taxBilled: Money,
    val discountTotal: Money,
    val dateSold: LocalDate,
    val dateVoided: LocalDate,
    val cogsTotal: Money
) : TransactionEvent() {
    val amountOwed: Money = taxableAmount + taxBilled
    val grossRevenue: Money = taxableAmount + discountTotal
}
