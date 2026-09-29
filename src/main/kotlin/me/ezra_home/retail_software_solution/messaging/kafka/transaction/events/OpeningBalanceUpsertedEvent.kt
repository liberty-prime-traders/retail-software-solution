package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class OpeningBalanceUpsertedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.OrgLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    val openingBalanceId: UUID,
    val ledgerSourceReferenceNumber: String,
    val accountCode: String,
    val accountEntryType: EntryType,
    val amount: BigDecimal,
    val postingDate: LocalDate
) : TransactionEvent() {
    override val sourceDocumentId: UUID get() = openingBalanceId
}
