package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import java.math.BigDecimal
import java.time.LocalDate

object LoanPostingRequests {

    fun loanRecorded(
        referenceNumber: String,
        assetAccountCode: String,
        lenderContactReferenceNumber: String,
        amount: BigDecimal,
        loanDate: LocalDate
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = referenceNumber,
            sourceType = LedgerSourceType.LOAN,
            postingDate = loanDate,
            entries = listOf(
                LedgerEntryRequest.debit(assetAccountCode, positiveAmount),
                LedgerEntryRequest.credit(SystemAccount.LOANS_PAYABLE.code, positiveAmount)
            ),
            subledgerEntries = listOf(payableRaised(lenderContactReferenceNumber, amount))
        )
    }

    // A void shares its original's reference. Reversals may build up an inactive account, so a void is never blocked by one.
    fun loanVoided(
        referenceNumber: String,
        assetAccountCode: String,
        lenderContactReferenceNumber: String,
        amount: BigDecimal,
        voidDate: LocalDate
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = referenceNumber,
            sourceType = LedgerSourceType.LOAN_VOID,
            postingDate = voidDate,
            entries = listOf(
                LedgerEntryRequest.debit(SystemAccount.LOANS_PAYABLE.code, positiveAmount),
                LedgerEntryRequest.credit(assetAccountCode, positiveAmount)
            ),
            subledgerEntries = listOf(receivableRaised(lenderContactReferenceNumber, amount)),
            reversal = true
        )
    }

    fun repaymentRecorded(
        referenceNumber: String,
        assetAccountCode: String,
        lenderContactReferenceNumber: String,
        amount: BigDecimal,
        repaymentDate: LocalDate
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = referenceNumber,
            sourceType = LedgerSourceType.LOAN_REPAYMENT,
            postingDate = repaymentDate,
            entries = listOf(
                LedgerEntryRequest.debit(SystemAccount.LOANS_PAYABLE.code, positiveAmount),
                LedgerEntryRequest.credit(assetAccountCode, positiveAmount)
            ),
            subledgerEntries = listOf(receivableRaised(lenderContactReferenceNumber, amount))
        )
    }

    fun repaymentVoided(
        referenceNumber: String,
        assetAccountCode: String,
        lenderContactReferenceNumber: String,
        amount: BigDecimal,
        voidDate: LocalDate
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = referenceNumber,
            sourceType = LedgerSourceType.LOAN_REPAYMENT_VOID,
            postingDate = voidDate,
            entries = listOf(
                LedgerEntryRequest.debit(assetAccountCode, positiveAmount),
                LedgerEntryRequest.credit(SystemAccount.LOANS_PAYABLE.code, positiveAmount)
            ),
            subledgerEntries = listOf(payableRaised(lenderContactReferenceNumber, amount)),
            reversal = true
        )
    }

    private fun payableRaised(contactReferenceNumber: String, amount: BigDecimal) =
        SubledgerEntryRequest(contactReferenceNumber, payableAmount = Money.of(amount), receivableAmount = Money.ZERO)

    private fun receivableRaised(contactReferenceNumber: String, amount: BigDecimal) =
        SubledgerEntryRequest(contactReferenceNumber, payableAmount = Money.ZERO, receivableAmount = Money.of(amount))
}
