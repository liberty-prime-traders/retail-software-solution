package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class SupplierPaymentRecordedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.LocationLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    val paymentId: UUID,
    val supplierId: UUID,
    val paymentMethodAccountCode: String,
    val amount: BigDecimal,
    val paymentDate: LocalDate,
    val paymentReferenceNumber: String
) : TransactionEvent() {
    override val sourceDocumentId: UUID get() = paymentId
}
