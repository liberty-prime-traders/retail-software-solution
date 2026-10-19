package me.ezra_home.retail_software_solution.locations.business.stock_adjustment

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount

data class StockAdjustmentLedgerAccounts(val debit: SystemAccount, val credit: SystemAccount)

object StockAdjustmentCategoryRules {

    fun fixedDirection(category: StockAdjustmentCategory): StockAdjustmentDirection? = when (category) {
        StockAdjustmentCategory.INTERNAL_USE -> StockAdjustmentDirection.DECREASE
        StockAdjustmentCategory.OPENING_STOCK, StockAdjustmentCategory.FREE_GOODS -> StockAdjustmentDirection.INCREASE
        StockAdjustmentCategory.OTHER -> null
    }

    fun displayName(category: StockAdjustmentCategory): String = when (category) {
        StockAdjustmentCategory.INTERNAL_USE -> "Internal Use"
        StockAdjustmentCategory.OPENING_STOCK -> "Opening Stock"
        StockAdjustmentCategory.FREE_GOODS -> "Free Goods"
        StockAdjustmentCategory.OTHER -> "Other"
    }

    fun allowedDirections(category: StockAdjustmentCategory): List<StockAdjustmentDirection> =
        fixedDirection(category)?.let { listOf(it) } ?: StockAdjustmentDirection.entries

    fun requiresUserSuppliedUnitCost(category: StockAdjustmentCategory): Boolean =
        category == StockAdjustmentCategory.OPENING_STOCK || category == StockAdjustmentCategory.FREE_GOODS

    fun requiresReason(category: StockAdjustmentCategory): Boolean = category == StockAdjustmentCategory.OTHER

    fun requiresNote(category: StockAdjustmentCategory): Boolean = !requiresReason(category)

    fun ledgerAccounts(category: StockAdjustmentCategory, direction: StockAdjustmentDirection): StockAdjustmentLedgerAccounts =
        when (category) {
            StockAdjustmentCategory.INTERNAL_USE ->
                StockAdjustmentLedgerAccounts(SystemAccount.INTERNAL_USE_EXPENSE, SystemAccount.INVENTORY)
            StockAdjustmentCategory.OPENING_STOCK ->
                StockAdjustmentLedgerAccounts(SystemAccount.INVENTORY, SystemAccount.OPENING_BALANCE_EQUITY)
            StockAdjustmentCategory.FREE_GOODS ->
                StockAdjustmentLedgerAccounts(SystemAccount.INVENTORY, SystemAccount.FREE_GOODS_RECEIVED)
            StockAdjustmentCategory.OTHER -> when (direction) {
                StockAdjustmentDirection.DECREASE ->
                    StockAdjustmentLedgerAccounts(SystemAccount.SHRINKAGE_AND_LOSSES, SystemAccount.INVENTORY)
                StockAdjustmentDirection.INCREASE ->
                    StockAdjustmentLedgerAccounts(SystemAccount.INVENTORY, SystemAccount.SHRINKAGE_AND_LOSSES)
            }
        }
}
