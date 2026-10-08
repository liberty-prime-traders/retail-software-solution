package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class ExpensePaymentRecordedAccountingProcessor(
    private val contactService: ContactService,
    private val expenseLedgerGate: ExpenseLedgerGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<ExpensePaymentRecordedEvent>(ledgerPostingService) {

    override val eventType: KClass<ExpensePaymentRecordedEvent> = ExpensePaymentRecordedEvent::class

    override val idempotencyConstraintName: String get() = expenseLedgerGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: ExpensePaymentRecordedEvent): Boolean =
        expenseLedgerGate.isPosted(event.sourceContext, event.paymentReferenceNumber, LedgerSourceType.EXPENSE_PAYMENT).not()

    override fun prepareLedgerRequest(event: ExpensePaymentRecordedEvent): LedgerPostingRequest {
        val payee = contactService.getContactById(event.payeeContactId)
        return ExpensePostingRequests.settle(
            sourceType = LedgerSourceType.EXPENSE_PAYMENT,
            paymentReference = event.paymentReferenceNumber,
            postingDate = DateTimes.Local.atOrganizationZone(event.paymentDate),
            expenseAccountCode = event.expenseAccountCode,
            paymentMethodAccountCode = event.paymentMethodAccountCode,
            amount = event.amount,
            payeeContactReferenceNumber = payee.referenceNumber
        )
    }
}
