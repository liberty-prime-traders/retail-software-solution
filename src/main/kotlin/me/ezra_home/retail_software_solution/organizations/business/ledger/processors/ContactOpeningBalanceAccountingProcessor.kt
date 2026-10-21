package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ContactOpeningBalanceUpsertedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import kotlin.reflect.KClass

@Service
class ContactOpeningBalanceAccountingProcessor(
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<ContactOpeningBalanceUpsertedEvent>(ledgerPostingService) {

    override val eventType: KClass<ContactOpeningBalanceUpsertedEvent> = ContactOpeningBalanceUpsertedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: ContactOpeningBalanceUpsertedEvent): Boolean =
        ledgerPostingGate.isPosted(
            event.sourceContext,
            event.ledgerSourceReferenceNumber,
            LedgerSourceType.CONTACT_OPENING_BALANCE
        ).not()

    override fun prepareLedgerRequest(event: ContactOpeningBalanceUpsertedEvent): LedgerPostingRequest {
        val sidesWhenRaised = sidesWhenRaised(event.subledgerDirection)
        val sides = if (event.balanceReduced) sidesWhenRaised.swapped() else sidesWhenRaised

        return LedgerPostingRequest(
            sourceReferenceNumber = event.ledgerSourceReferenceNumber,
            sourceType = LedgerSourceType.CONTACT_OPENING_BALANCE,
            postingDate = event.postingDate,
            entries = listOf(
                LedgerEntryRequest.debit(sides.debitAccountCode, event.amount),
                LedgerEntryRequest.credit(sides.creditAccountCode, event.amount)
            ),
            subledgerEntries = listOf(subledgerEntry(event))
        )
    }

    private fun sidesWhenRaised(subledgerDirection: ContactSubledgerDirection): LedgerSides =
        when (subledgerDirection) {
            ContactSubledgerDirection.RECEIVABLE -> LedgerSides(
                debitAccountCode = SystemAccount.TRADE_RECEIVABLES.code,
                creditAccountCode = SystemAccount.OPENING_BALANCE_EQUITY.code
            )
            ContactSubledgerDirection.PAYABLE -> LedgerSides(
                debitAccountCode = SystemAccount.OPENING_BALANCE_EQUITY.code,
                creditAccountCode = SystemAccount.TRADE_PAYABLES.code
            )
        }

    private fun subledgerEntry(event: ContactOpeningBalanceUpsertedEvent): SubledgerEntryRequest =
        if (contactOwesUsMore(event)) {
            SubledgerEntryRequest(
                contactReferenceNumber = event.contactReferenceNumber,
                payableAmount = BigDecimal.ZERO,
                receivableAmount = event.amount
            )
        } else {
            SubledgerEntryRequest(
                contactReferenceNumber = event.contactReferenceNumber,
                payableAmount = event.amount,
                receivableAmount = BigDecimal.ZERO
            )
        }

    private fun contactOwesUsMore(event: ContactOpeningBalanceUpsertedEvent): Boolean =
        when (event.subledgerDirection) {
            ContactSubledgerDirection.RECEIVABLE -> !event.balanceReduced
            ContactSubledgerDirection.PAYABLE -> event.balanceReduced
        }

    private data class LedgerSides(val debitAccountCode: String, val creditAccountCode: String) {
        fun swapped() = LedgerSides(debitAccountCode = creditAccountCode, creditAccountCode = debitAccountCode)
    }
}
