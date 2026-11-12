package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import java.math.BigDecimal
import java.time.LocalDate

object ContactOpeningSettlementPostingRequests {

    fun recorded(settlementEntity: ContactOpeningSettlementEntity): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(settlementEntity.amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = settlementEntity.requiredReference(),
            sourceType = LedgerSourceType.CONTACT_OPENING_SETTLEMENT,
            postingDate = settlementEntity.settlementDate,
            entries = when (settlementEntity.subledgerDirection) {
                ContactSubledgerDirection.RECEIVABLE -> listOf(
                    LedgerEntryRequest.debit(settlementEntity.paymentAccountCode, positiveAmount),
                    LedgerEntryRequest.credit(SystemAccount.TRADE_RECEIVABLES.code, positiveAmount)
                )
                ContactSubledgerDirection.PAYABLE -> listOf(
                    LedgerEntryRequest.debit(SystemAccount.TRADE_PAYABLES.code, positiveAmount),
                    LedgerEntryRequest.credit(settlementEntity.paymentAccountCode, positiveAmount)
                )
            },
            subledgerEntries = listOf(
                subledgerEntry(
                    settlementEntity.contactReferenceNumber,
                    settlementEntity.subledgerDirection,
                    settlementEntity.amount
                )
            )
        )
    }

    fun voided(settlementEntity: ContactOpeningSettlementEntity, voidDate: LocalDate): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(settlementEntity.amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = settlementEntity.requiredReference(),
            sourceType = LedgerSourceType.CONTACT_OPENING_SETTLEMENT_VOID,
            postingDate = voidDate,
            entries = when (settlementEntity.subledgerDirection) {
                ContactSubledgerDirection.RECEIVABLE -> listOf(
                    LedgerEntryRequest.debit(SystemAccount.TRADE_RECEIVABLES.code, positiveAmount),
                    LedgerEntryRequest.credit(settlementEntity.paymentAccountCode, positiveAmount)
                )
                ContactSubledgerDirection.PAYABLE -> listOf(
                    LedgerEntryRequest.debit(settlementEntity.paymentAccountCode, positiveAmount),
                    LedgerEntryRequest.credit(SystemAccount.TRADE_PAYABLES.code, positiveAmount)
                )
            },
            subledgerEntries = listOf(
                subledgerEntry(
                    settlementEntity.contactReferenceNumber,
                    settlementEntity.subledgerDirection.opposite(),
                    settlementEntity.amount
                )
            ),
            reversal = true
        )
    }

    private fun subledgerEntry(
        contactReferenceNumber: String,
        settledDirection: ContactSubledgerDirection,
        amount: BigDecimal
    ): SubledgerEntryRequest = when (settledDirection) {
        ContactSubledgerDirection.RECEIVABLE ->
            SubledgerEntryRequest(contactReferenceNumber, payableAmount = Money.of(amount), receivableAmount = Money.ZERO)
        ContactSubledgerDirection.PAYABLE ->
            SubledgerEntryRequest(contactReferenceNumber, payableAmount = Money.ZERO, receivableAmount = Money.of(amount))
    }
}
