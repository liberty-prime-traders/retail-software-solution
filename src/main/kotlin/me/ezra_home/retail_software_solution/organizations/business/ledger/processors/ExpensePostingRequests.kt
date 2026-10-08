package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import java.math.BigDecimal
import java.time.LocalDate

object ExpensePostingRequests {

    fun incur(
        sourceType: LedgerSourceType,
        reference: String,
        postingDate: LocalDate,
        expenseAccountCode: String,
        amount: BigDecimal,
        payeeContactReferenceNumber: String
    ): LedgerPostingRequest = LedgerPostingRequest(
        sourceReferenceNumber = reference,
        sourceType = sourceType,
        postingDate = postingDate,
        entries = listOf(
            LedgerEntryRequest.debit(expenseAccountCode, amount),
            LedgerEntryRequest.credit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), amount)
        ),
        subledgerEntries = listOf(payable(payeeContactReferenceNumber, amount))
    )

    fun reverseIncur(
        sourceType: LedgerSourceType,
        reference: String,
        postingDate: LocalDate,
        expenseAccountCode: String,
        amount: BigDecimal,
        payeeContactReferenceNumber: String
    ): LedgerPostingRequest = LedgerPostingRequest(
        sourceReferenceNumber = reference,
        sourceType = sourceType,
        postingDate = postingDate,
        entries = listOf(
            LedgerEntryRequest.credit(expenseAccountCode, amount),
            LedgerEntryRequest.debit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), amount)
        ),
        subledgerEntries = listOf(settled(payeeContactReferenceNumber, amount))
    )

    fun settle(
        sourceType: LedgerSourceType,
        paymentReference: String,
        postingDate: LocalDate,
        expenseAccountCode: String,
        paymentMethodAccountCode: String,
        amount: BigDecimal,
        payeeContactReferenceNumber: String
    ): LedgerPostingRequest = LedgerPostingRequest(
        sourceReferenceNumber = paymentReference,
        sourceType = sourceType,
        postingDate = postingDate,
        entries = listOf(
            LedgerEntryRequest.debit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), amount),
            LedgerEntryRequest.credit(paymentMethodAccountCode, amount)
        ),
        subledgerEntries = listOf(settled(payeeContactReferenceNumber, amount))
    )

    fun reverseSettle(
        sourceType: LedgerSourceType,
        paymentReference: String,
        postingDate: LocalDate,
        expenseAccountCode: String,
        paymentMethodAccountCode: String,
        amount: BigDecimal,
        payeeContactReferenceNumber: String
    ): LedgerPostingRequest = LedgerPostingRequest(
        sourceReferenceNumber = paymentReference,
        sourceType = sourceType,
        postingDate = postingDate,
        entries = listOf(
            LedgerEntryRequest.credit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), amount),
            LedgerEntryRequest.debit(paymentMethodAccountCode, amount)
        ),
        subledgerEntries = listOf(payable(payeeContactReferenceNumber, amount))
    )

    private fun payable(contactReferenceNumber: String, amount: BigDecimal) =
        SubledgerEntryRequest(contactReferenceNumber, payableAmount = amount, receivableAmount = BigDecimal.ZERO)

    private fun settled(contactReferenceNumber: String, amount: BigDecimal) =
        SubledgerEntryRequest(contactReferenceNumber, payableAmount = BigDecimal.ZERO, receivableAmount = amount)
}
