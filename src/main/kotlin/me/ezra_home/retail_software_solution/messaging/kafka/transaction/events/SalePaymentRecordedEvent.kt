package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

data class SalePaymentRecordedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.LocationLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    override val sourceDocumentId: UUID,
    val contactId: UUID,
    val payments: List<SalePaymentLineDto>
) : TransactionEvent()

data class SalePaymentLineDto(
    val paymentReferenceNumber: String,
    val paymentMethodAccountCode: String,
    val amount: BigDecimal,
    val paymentDate: OffsetDateTime
)
