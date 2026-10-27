package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
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
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = reference,
            sourceType = sourceType,
            postingDate = postingDate,
            entries = listOf(
                LedgerEntryRequest.debit(expenseAccountCode, positiveAmount),
                LedgerEntryRequest.credit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), positiveAmount)
            ),
            subledgerEntries = listOf(payable(payeeContactReferenceNumber, amount))
        )
    }

    fun reverseIncur(
        sourceType: LedgerSourceType,
        reference: String,
        postingDate: LocalDate,
        expenseAccountCode: String,
        amount: BigDecimal,
        payeeContactReferenceNumber: String
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = reference,
            sourceType = sourceType,
            postingDate = postingDate,
            entries = listOf(
                LedgerEntryRequest.credit(expenseAccountCode, positiveAmount),
                LedgerEntryRequest.debit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), positiveAmount)
            ),
            subledgerEntries = listOf(settled(payeeContactReferenceNumber, amount))
        )
    }

    fun settle(
        sourceType: LedgerSourceType,
        paymentReference: String,
        postingDate: LocalDate,
        expenseAccountCode: String,
        paymentMethodAccountCode: String,
        amount: BigDecimal,
        payeeContactReferenceNumber: String
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = paymentReference,
            sourceType = sourceType,
            postingDate = postingDate,
            entries = listOf(
                LedgerEntryRequest.debit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), positiveAmount),
                LedgerEntryRequest.credit(paymentMethodAccountCode, positiveAmount)
            ),
            subledgerEntries = listOf(settled(payeeContactReferenceNumber, amount))
        )
    }

    fun reverseSettle(
        sourceType: LedgerSourceType,
        paymentReference: String,
        postingDate: LocalDate,
        expenseAccountCode: String,
        paymentMethodAccountCode: String,
        amount: BigDecimal,
        payeeContactReferenceNumber: String
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(amount)
        return LedgerPostingRequest(
            sourceReferenceNumber = paymentReference,
            sourceType = sourceType,
            postingDate = postingDate,
            entries = listOf(
                LedgerEntryRequest.credit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), positiveAmount),
                LedgerEntryRequest.debit(paymentMethodAccountCode, positiveAmount)
            ),
            subledgerEntries = listOf(payable(payeeContactReferenceNumber, amount))
        )
    }

    private fun payable(contactReferenceNumber: String, amount: BigDecimal) =
        SubledgerEntryRequest(contactReferenceNumber, payableAmount = Money.of(amount), receivableAmount = Money.ZERO)

    private fun settled(contactReferenceNumber: String, amount: BigDecimal) =
        SubledgerEntryRequest(contactReferenceNumber, payableAmount = Money.ZERO, receivableAmount = Money.of(amount))
}
