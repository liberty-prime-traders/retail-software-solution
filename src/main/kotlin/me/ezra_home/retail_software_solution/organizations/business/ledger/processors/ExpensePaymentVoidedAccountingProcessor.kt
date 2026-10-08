package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentVoidedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class ExpensePaymentVoidedAccountingProcessor(
    private val contactService: ContactService,
    private val expenseLedgerGate: ExpenseLedgerGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<ExpensePaymentVoidedEvent>(ledgerPostingService) {

    override val eventType: KClass<ExpensePaymentVoidedEvent> = ExpensePaymentVoidedEvent::class

    override val idempotencyConstraintName: String get() = expenseLedgerGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: ExpensePaymentVoidedEvent): Boolean =
        expenseLedgerGate.isPosted(event.sourceContext, event.paymentReferenceNumber, LedgerSourceType.EXPENSE_PAYMENT) &&
            expenseLedgerGate.isPosted(event.sourceContext, event.paymentReferenceNumber, LedgerSourceType.EXPENSE_PAYMENT_VOID).not()

    override fun prepareLedgerRequest(event: ExpensePaymentVoidedEvent): LedgerPostingRequest {
        val payee = contactService.getContactById(event.payeeContactId)
        return ExpensePostingRequests.reverseSettle(
            sourceType = LedgerSourceType.EXPENSE_PAYMENT_VOID,
            paymentReference = event.paymentReferenceNumber,
            postingDate = event.voidedOn,
            expenseAccountCode = event.expenseAccountCode,
            paymentMethodAccountCode = event.paymentMethodAccountCode,
            amount = event.amount,
            payeeContactReferenceNumber = payee.referenceNumber
        )
    }
}
