package me.ezra_home.retail_software_solution.cross_tier.expense

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class ExpenseSourceType(override val code: String) : HasCode {
    PURCHASE("PUR"),
    SALE("SAL"),
    STOCK_TRANSFER("TRF"),
    WAGES("WAG"),
    ADHOC("ADH");

    fun batchDescriptionLabel(): String = when (this) {
        PURCHASE -> "Purchase"
        SALE -> "Sale"
        STOCK_TRANSFER -> "Stock transfer"
        WAGES -> "Wages"
        ADHOC -> "Expenses"
    }
}
