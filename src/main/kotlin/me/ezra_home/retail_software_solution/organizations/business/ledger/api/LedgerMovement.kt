package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import java.math.BigDecimal
import java.time.LocalDate

data class LedgerMovement(
    val localDate: LocalDate,
    val accountCode: String,
    val entryType: EntryType,
    val source: LedgerSourceType,
    val amount: BigDecimal
)

data class LedgerAccountMovement(
    val localDate: LocalDate,
    val accountCode: String,
    val entryType: EntryType,
    val amount: BigDecimal
)
