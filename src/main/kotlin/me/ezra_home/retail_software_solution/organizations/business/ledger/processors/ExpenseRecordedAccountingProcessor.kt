package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class ExpenseRecordedAccountingProcessor(
    private val contactService: ContactService,
    private val expenseLedgerGate: ExpenseLedgerGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<ExpenseRecordedEvent>(ledgerPostingService) {

    override val eventType: KClass<ExpenseRecordedEvent> = ExpenseRecordedEvent::class

    override val idempotencyConstraintName: String get() = expenseLedgerGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: ExpenseRecordedEvent): Boolean =
        expenseLedgerGate.isPosted(event.sourceContext, event.expenseReferenceNumber, LedgerSourceType.EXPENSE).not()

    override fun prepareLedgerRequest(event: ExpenseRecordedEvent): LedgerPostingRequest {
        val payee = contactService.getContactById(event.payeeContactId)
        return ExpensePostingRequests.incur(
            sourceType = LedgerSourceType.EXPENSE,
            reference = event.expenseReferenceNumber,
            postingDate = event.expenseDate,
            expenseAccountCode = event.expenseAccountCode,
            amount = event.amount,
            payeeContactReferenceNumber = payee.referenceNumber
        )
    }
}
