package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import java.time.LocalDate

object ExpensePostingRequests {

    fun incur(
        sourceType: LedgerSourceType,
        reference: String,
        postingDate: LocalDate,
        expenseAccountCode: String,
        amount: PositiveMoney,
        payeeContactReferenceNumber: String
    ): LedgerPostingRequest {
        return LedgerPostingRequest(
            sourceReferenceNumber = reference,
            sourceType = sourceType,
            postingDate = postingDate,
            entries = listOf(
                LedgerEntryRequest.debit(expenseAccountCode, amount),
                LedgerEntryRequest.credit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), amount)
            ),
            subledgerEntries = listOf(payable(payeeContactReferenceNumber, amount))
        )
    }

    fun reverseIncur(
        sourceType: LedgerSourceType,
        reference: String,
        postingDate: LocalDate,
        expenseAccountCode: String,
        amount: PositiveMoney,
        payeeContactReferenceNumber: String
    ): LedgerPostingRequest {
        return LedgerPostingRequest(
            sourceReferenceNumber = reference,
            sourceType = sourceType,
            postingDate = postingDate,
            entries = listOf(
                LedgerEntryRequest.credit(expenseAccountCode, amount),
                LedgerEntryRequest.debit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), amount)
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
        amount: PositiveMoney,
        payeeContactReferenceNumber: String
    ): LedgerPostingRequest {
        return LedgerPostingRequest(
            sourceReferenceNumber = paymentReference,
            sourceType = sourceType,
            postingDate = postingDate,
            entries = listOf(
                LedgerEntryRequest.debit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), amount),
                LedgerEntryRequest.credit(paymentMethodAccountCode, amount)
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
        amount: PositiveMoney,
        payeeContactReferenceNumber: String
    ): LedgerPostingRequest {
        return LedgerPostingRequest(
            sourceReferenceNumber = paymentReference,
            sourceType = sourceType,
            postingDate = postingDate,
            entries = listOf(
                LedgerEntryRequest.credit(ExpenseLiabilityAccount.forExpenseAccount(expenseAccountCode), amount),
                LedgerEntryRequest.debit(paymentMethodAccountCode, amount)
            ),
            subledgerEntries = listOf(payable(payeeContactReferenceNumber, amount))
        )
    }

    private fun payable(contactReferenceNumber: String, amount: PositiveMoney) =
        SubledgerEntryRequest(contactReferenceNumber, payableAmount = Money.of(amount.toBigDecimal()), receivableAmount = Money.ZERO)

    private fun settled(contactReferenceNumber: String, amount: PositiveMoney) =
        SubledgerEntryRequest(contactReferenceNumber, payableAmount = Money.ZERO, receivableAmount = Money.of(amount.toBigDecimal()))
}
