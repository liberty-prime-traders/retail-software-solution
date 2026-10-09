package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import java.util.UUID

abstract class ExpenseAccountingProcessor<EVENT : TransactionEvent>(
    private val contactService: ContactService,
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<EVENT>(ledgerPostingService) {

    protected abstract val sourceType: LedgerSourceType

    // Set by voids: the posting they reverse, which shares the void's reference and differs only by source type.
    protected open val reversedSourceType: LedgerSourceType? = null

    protected abstract fun referenceNumberOf(event: EVENT): String

    protected abstract fun payeeContactIdOf(event: EVENT): UUID

    protected abstract fun buildRequest(event: EVENT, payeeContactReferenceNumber: String): LedgerPostingRequest

    override val idempotencyConstraintName: String get() = ledgerPostingGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: EVENT): Boolean =
        ledgerPostingGate.isPosted(event.sourceContext, referenceNumberOf(event), sourceType).not()

    override fun prepareLedgerRequest(event: EVENT): LedgerPostingRequest {
        reversedSourceType?.let { ledgerPostingGate.requirePosted(event.sourceContext, referenceNumberOf(event), it) }
        val payee = contactService.getContactById(payeeContactIdOf(event))
        return buildRequest(event, payee.referenceNumber)
    }
}
