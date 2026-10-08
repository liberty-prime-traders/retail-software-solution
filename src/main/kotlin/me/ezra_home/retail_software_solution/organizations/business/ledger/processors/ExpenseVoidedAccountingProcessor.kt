package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseVoidedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class ExpenseVoidedAccountingProcessor(
    private val contactService: ContactService,
    private val expenseLedgerGate: ExpenseLedgerGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<ExpenseVoidedEvent>(ledgerPostingService) {

    override val eventType: KClass<ExpenseVoidedEvent> = ExpenseVoidedEvent::class

    override val idempotencyConstraintName: String get() = expenseLedgerGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: ExpenseVoidedEvent): Boolean =
        expenseLedgerGate.isPosted(event.sourceContext, event.expenseReferenceNumber, LedgerSourceType.EXPENSE) &&
            expenseLedgerGate.isPosted(event.sourceContext, event.expenseReferenceNumber, LedgerSourceType.EXPENSE_VOID).not()

    override fun prepareLedgerRequest(event: ExpenseVoidedEvent): LedgerPostingRequest {
        val payee = contactService.getContactById(event.payeeContactId)
        return ExpensePostingRequests.reverseIncur(
            sourceType = LedgerSourceType.EXPENSE_VOID,
            reference = event.expenseReferenceNumber,
            postingDate = event.voidedOn,
            expenseAccountCode = event.expenseAccountCode,
            amount = event.amount,
            payeeContactReferenceNumber = payee.referenceNumber
        )
    }
}
