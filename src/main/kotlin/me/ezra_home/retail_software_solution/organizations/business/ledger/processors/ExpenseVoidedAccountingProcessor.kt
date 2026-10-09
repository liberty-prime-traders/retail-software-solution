package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseVoidedEvent
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.reflect.KClass

@Service
class ExpenseVoidedAccountingProcessor(
    contactService: ContactService,
    ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : ExpenseAccountingProcessor<ExpenseVoidedEvent>(contactService, ledgerPostingGate, ledgerPostingService) {

    override val eventType: KClass<ExpenseVoidedEvent> = ExpenseVoidedEvent::class

    override val sourceType = LedgerSourceType.EXPENSE_VOID

    override val reversedSourceType = LedgerSourceType.EXPENSE

    override fun referenceNumberOf(event: ExpenseVoidedEvent): String = event.expenseReferenceNumber

    override fun payeeContactIdOf(event: ExpenseVoidedEvent): UUID = event.payeeContactId

    override fun buildRequest(event: ExpenseVoidedEvent, payeeContactReferenceNumber: String): LedgerPostingRequest =
        ExpensePostingRequests.reverseIncur(
            sourceType = sourceType,
            reference = event.expenseReferenceNumber,
            postingDate = event.voidedOn,
            expenseAccountCode = event.expenseAccountCode,
            amount = event.amount,
            payeeContactReferenceNumber = payeeContactReferenceNumber
        )
}
