package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import java.math.BigDecimal
import java.time.LocalDate

data class LedgerPostingRequest(
    val sourceReferenceNumber: String,
    val sourceType: LedgerSourceType,
    val postingDate: LocalDate,
    val entries: List<LedgerEntryRequest>,
    val subledgerEntries: List<SubledgerEntryRequest>,
    val reversal: Boolean = false
)

data class LedgerEntryRequest(
    val accountCode: String,
    val entryType: EntryType,
    val amount: PositiveMoney
) {
    companion object {
        fun debit(accountCode: String, amount: PositiveMoney) = LedgerEntryRequest(accountCode, EntryType.DEBIT, amount)
        fun credit(accountCode: String, amount: PositiveMoney) = LedgerEntryRequest(accountCode, EntryType.CREDIT, amount)

        // A zero leg carries no information and PositiveMoney rejects it, so it is omitted rather than posted.
        // A negative amount is not skipped: it reaches PositiveMoney.of and fails loudly.
        fun debitUnlessZero(accountCode: String, amount: BigDecimal): List<LedgerEntryRequest> =
            unlessZero(accountCode, EntryType.DEBIT, amount)

        fun creditUnlessZero(accountCode: String, amount: BigDecimal): List<LedgerEntryRequest> =
            unlessZero(accountCode, EntryType.CREDIT, amount)

        fun unlessZero(accountCode: String, entryType: EntryType, amount: BigDecimal): List<LedgerEntryRequest> =
            if (amount.signum() == 0) emptyList() else listOf(LedgerEntryRequest(accountCode, entryType, PositiveMoney.of(amount)))
    }
}

data class SubledgerEntryRequest(
    val contactReferenceNumber: String,
    val payableAmount: Money,
    val receivableAmount: Money
)
