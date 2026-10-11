package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ContactOpeningBalanceUpsertedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.OrgLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    val contactOpeningBalanceId: UUID,
    val ledgerSourceReferenceNumber: String,
    val contactReferenceNumber: String,
    val subledgerDirection: ContactSubledgerDirection,
    val amount: BigDecimal,
    val balanceReduced: Boolean,
    val postingDate: LocalDate
) : TransactionEvent() {
    override val sourceDocumentId: UUID = contactOpeningBalanceId
}
