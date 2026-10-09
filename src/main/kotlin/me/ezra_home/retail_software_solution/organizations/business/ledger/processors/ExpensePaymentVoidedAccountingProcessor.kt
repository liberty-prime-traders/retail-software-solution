package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentVoidedEvent
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.reflect.KClass

@Service
class ExpensePaymentVoidedAccountingProcessor(
    contactService: ContactService,
    ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : ExpenseAccountingProcessor<ExpensePaymentVoidedEvent>(contactService, ledgerPostingGate, ledgerPostingService) {

    override val eventType: KClass<ExpensePaymentVoidedEvent> = ExpensePaymentVoidedEvent::class

    override val sourceType = LedgerSourceType.EXPENSE_PAYMENT_VOID

    override val reversedSourceType = LedgerSourceType.EXPENSE_PAYMENT

    override fun referenceNumberOf(event: ExpensePaymentVoidedEvent): String = event.paymentReferenceNumber

    override fun payeeContactIdOf(event: ExpensePaymentVoidedEvent): UUID = event.payeeContactId

    override fun buildRequest(event: ExpensePaymentVoidedEvent, payeeContactReferenceNumber: String): LedgerPostingRequest =
        ExpensePostingRequests.reverseSettle(
            sourceType = sourceType,
            paymentReference = event.paymentReferenceNumber,
            postingDate = event.voidedOn,
            expenseAccountCode = event.expenseAccountCode,
            paymentMethodAccountCode = event.paymentMethodAccountCode,
            amount = event.amount,
            payeeContactReferenceNumber = payeeContactReferenceNumber
        )
}
