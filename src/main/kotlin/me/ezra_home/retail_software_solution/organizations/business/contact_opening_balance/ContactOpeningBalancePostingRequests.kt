package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import java.math.BigDecimal
import java.time.LocalDate

object ContactOpeningBalancePostingRequests {

    fun revised(
        ledgerSourceReferenceNumber: String,
        contactReferenceNumber: String,
        subledgerDirection: ContactSubledgerDirection,
        signedDelta: BigDecimal,
        postingDate: LocalDate
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(signedDelta.abs())
        val balanceReduced = signedDelta.signum() < 0
        val sidesWhenRaised = sidesWhenRaised(subledgerDirection)
        val sides = if (balanceReduced) sidesWhenRaised.swapped() else sidesWhenRaised

        return LedgerPostingRequest(
            sourceReferenceNumber = ledgerSourceReferenceNumber,
            sourceType = LedgerSourceType.CONTACT_OPENING_BALANCE,
            postingDate = postingDate,
            entries = listOf(
                LedgerEntryRequest.debit(sides.debitAccountCode, positiveAmount),
                LedgerEntryRequest.credit(sides.creditAccountCode, positiveAmount)
            ),
            subledgerEntries = listOf(
                subledgerEntry(contactReferenceNumber, subledgerDirection, balanceReduced, signedDelta.abs())
            )
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

    private fun subledgerEntry(
        contactReferenceNumber: String,
        subledgerDirection: ContactSubledgerDirection,
        balanceReduced: Boolean,
        amount: BigDecimal
    ): SubledgerEntryRequest {
        val contactOwesUsMore = when (subledgerDirection) {
            ContactSubledgerDirection.RECEIVABLE -> !balanceReduced
            ContactSubledgerDirection.PAYABLE -> balanceReduced
        }
        return if (contactOwesUsMore) {
            SubledgerEntryRequest(contactReferenceNumber, payableAmount = Money.ZERO, receivableAmount = Money.of(amount))
        } else {
            SubledgerEntryRequest(contactReferenceNumber, payableAmount = Money.of(amount), receivableAmount = Money.ZERO)
        }
    }

    private data class LedgerSides(val debitAccountCode: String, val creditAccountCode: String) {
        fun swapped() = LedgerSides(debitAccountCode = creditAccountCode, creditAccountCode = debitAccountCode)
    }
}
