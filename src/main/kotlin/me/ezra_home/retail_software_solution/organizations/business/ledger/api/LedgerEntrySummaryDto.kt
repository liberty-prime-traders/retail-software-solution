package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import java.math.BigDecimal

data class LedgerEntrySummaryDto(
    val accountCode: String,
    val amount: BigDecimal,
    val entryType: EntryType
)
