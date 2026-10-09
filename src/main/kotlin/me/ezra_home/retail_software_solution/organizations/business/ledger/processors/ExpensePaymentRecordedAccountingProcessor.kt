package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentRecordedEvent
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.reflect.KClass

@Service
class ExpensePaymentRecordedAccountingProcessor(
    contactService: ContactService,
    ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : ExpenseAccountingProcessor<ExpensePaymentRecordedEvent>(contactService, ledgerPostingGate, ledgerPostingService) {

    override val eventType: KClass<ExpensePaymentRecordedEvent> = ExpensePaymentRecordedEvent::class

    override val sourceType = LedgerSourceType.EXPENSE_PAYMENT

    override fun referenceNumberOf(event: ExpensePaymentRecordedEvent): String = event.paymentReferenceNumber

    override fun payeeContactIdOf(event: ExpensePaymentRecordedEvent): UUID = event.payeeContactId

    override fun buildRequest(event: ExpensePaymentRecordedEvent, payeeContactReferenceNumber: String): LedgerPostingRequest =
        ExpensePostingRequests.settle(
            sourceType = sourceType,
            paymentReference = event.paymentReferenceNumber,
            postingDate = DateTimes.Local.atOrganizationZone(event.paymentDate),
            expenseAccountCode = event.expenseAccountCode,
            paymentMethodAccountCode = event.paymentMethodAccountCode,
            amount = event.amount,
            payeeContactReferenceNumber = payeeContactReferenceNumber
        )
}
