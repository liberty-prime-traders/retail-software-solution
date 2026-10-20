package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
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
    val amount: BigDecimal
) {
    companion object {
        fun debit(accountCode: String, amount: BigDecimal) = LedgerEntryRequest(accountCode, EntryType.DEBIT, amount)
        fun credit(accountCode: String, amount: BigDecimal) = LedgerEntryRequest(accountCode, EntryType.CREDIT, amount)
    }
}

data class SubledgerEntryRequest(
    val contactReferenceNumber: String,
    val payableAmount: BigDecimal,
    val receivableAmount: BigDecimal
)
