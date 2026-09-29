package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import java.math.BigDecimal
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
    val payableTotal: BigDecimal,
    val discountTotal: BigDecimal,
    val dateSold: LocalDate,
    val dateVoided: LocalDate
) : TransactionEvent()
