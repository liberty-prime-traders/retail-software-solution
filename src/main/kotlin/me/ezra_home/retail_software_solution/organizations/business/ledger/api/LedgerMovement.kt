package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import java.math.BigDecimal
import java.time.LocalDate

enum class LedgerMovementSource {
    SALE,
    SALE_VOID,
    SALE_PAYMENT,
    SALE_PAYMENT_VOID,
    EXPENSE,
    EXPENSE_VOID,
    STOCK_ADJUSTMENT
}

data class LedgerMovement(
    val localDate: LocalDate,
    val accountCode: String,
    val entryType: EntryType,
    val source: LedgerMovementSource,
    val amount: BigDecimal
)

data class LedgerAccountMovement(
    val localDate: LocalDate,
    val accountCode: String,
    val entryType: EntryType,
    val amount: BigDecimal
)
