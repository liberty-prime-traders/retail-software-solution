package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ExpenseRecordedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    val expenseId: UUID,
    val expenseReferenceNumber: String,
    val expenseAccountCode: String,
    val payeeContactId: UUID,
    val amount: BigDecimal,
    val expenseDate: LocalDate
) : TransactionEvent() {
    override val sourceDocumentId: UUID get() = expenseId
}
