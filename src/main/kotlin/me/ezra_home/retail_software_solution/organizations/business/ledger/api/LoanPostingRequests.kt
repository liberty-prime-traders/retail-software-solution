package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import java.math.BigDecimal
import java.time.LocalDate

object LoanPostingRequests {

    fun loanRecorded(
        referenceNumber: String,
        assetAccountCode: String,
        lenderContactReferenceNumber: String,
        amount: BigDecimal,
        loanDate: LocalDate
    ): LedgerPostingRequest = LedgerPostingRequest(
        sourceReferenceNumber = referenceNumber,
        sourceType = LedgerSourceType.LOAN,
        postingDate = loanDate,
        entries = listOf(
            LedgerEntryRequest.debit(assetAccountCode, amount),
            LedgerEntryRequest.credit(SystemAccount.LOANS_PAYABLE.code, amount)
        ),
        subledgerEntries = listOf(payableRaised(lenderContactReferenceNumber, amount))
    )

    // A void shares its original's reference. Reversals may build up an inactive account, so a void is never blocked by one.
    fun loanVoided(
        referenceNumber: String,
        assetAccountCode: String,
        lenderContactReferenceNumber: String,
        amount: BigDecimal,
        voidDate: LocalDate
    ): LedgerPostingRequest = LedgerPostingRequest(
        sourceReferenceNumber = referenceNumber,
        sourceType = LedgerSourceType.LOAN_VOID,
        postingDate = voidDate,
        entries = listOf(
            LedgerEntryRequest.debit(SystemAccount.LOANS_PAYABLE.code, amount),
            LedgerEntryRequest.credit(assetAccountCode, amount)
        ),
        subledgerEntries = listOf(receivableRaised(lenderContactReferenceNumber, amount)),
        reversal = true
    )

    fun repaymentRecorded(
        referenceNumber: String,
        assetAccountCode: String,
        lenderContactReferenceNumber: String,
        amount: BigDecimal,
        repaymentDate: LocalDate
    ): LedgerPostingRequest = LedgerPostingRequest(
        sourceReferenceNumber = referenceNumber,
        sourceType = LedgerSourceType.LOAN_REPAYMENT,
        postingDate = repaymentDate,
        entries = listOf(
            LedgerEntryRequest.debit(SystemAccount.LOANS_PAYABLE.code, amount),
            LedgerEntryRequest.credit(assetAccountCode, amount)
        ),
        subledgerEntries = listOf(receivableRaised(lenderContactReferenceNumber, amount))
    )

    fun repaymentVoided(
        referenceNumber: String,
        assetAccountCode: String,
        lenderContactReferenceNumber: String,
        amount: BigDecimal,
        voidDate: LocalDate
    ): LedgerPostingRequest = LedgerPostingRequest(
        sourceReferenceNumber = referenceNumber,
        sourceType = LedgerSourceType.LOAN_REPAYMENT_VOID,
        postingDate = voidDate,
        entries = listOf(
            LedgerEntryRequest.debit(assetAccountCode, amount),
            LedgerEntryRequest.credit(SystemAccount.LOANS_PAYABLE.code, amount)
        ),
        subledgerEntries = listOf(payableRaised(lenderContactReferenceNumber, amount)),
        reversal = true
    )

    private fun payableRaised(contactReferenceNumber: String, amount: BigDecimal) =
        SubledgerEntryRequest(contactReferenceNumber, payableAmount = amount, receivableAmount = BigDecimal.ZERO)

    private fun receivableRaised(contactReferenceNumber: String, amount: BigDecimal) =
        SubledgerEntryRequest(contactReferenceNumber, payableAmount = BigDecimal.ZERO, receivableAmount = amount)
}
