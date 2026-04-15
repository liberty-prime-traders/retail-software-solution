package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import java.math.BigDecimal
import java.time.LocalDate

data class LedgerPostingRequest(
    val sourceReferenceNumber: String,
    val sourceType: LedgerSourceType,
    val postingDate: LocalDate,
    val entries: List<LedgerEntryRequest>,
    val subledgerEntry: SubledgerEntryRequest?
)

data class LedgerEntryRequest(
    val accountCode: String,
    val entryType: EntryType,
    val amount: BigDecimal
)

data class SubledgerEntryRequest(
    val contactReferenceNumber: String,
    val payableAmount: BigDecimal,
    val receivableAmount: BigDecimal
)
