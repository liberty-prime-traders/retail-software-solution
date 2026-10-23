package me.ezra_home.retail_software_solution.locations.business.stock_adjustment

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StockAdjustmentCategoryRulesTest {

    @Test
    fun `each category posts to its fixed account pair and other flips with the direction`() {
        val expectedAccountsByCategoryAndDirection = mapOf(
            (StockAdjustmentCategory.INTERNAL_USE to StockAdjustmentDirection.DECREASE) to
                StockAdjustmentLedgerAccounts(SystemAccount.INTERNAL_USE_EXPENSE, SystemAccount.INVENTORY),
            (StockAdjustmentCategory.OPENING_STOCK to StockAdjustmentDirection.INCREASE) to
                StockAdjustmentLedgerAccounts(SystemAccount.INVENTORY, SystemAccount.OPENING_BALANCE_EQUITY),
            (StockAdjustmentCategory.FREE_GOODS to StockAdjustmentDirection.INCREASE) to
                StockAdjustmentLedgerAccounts(SystemAccount.INVENTORY, SystemAccount.FREE_GOODS_RECEIVED),
            (StockAdjustmentCategory.OTHER to StockAdjustmentDirection.DECREASE) to
                StockAdjustmentLedgerAccounts(SystemAccount.SHRINKAGE_AND_LOSSES, SystemAccount.INVENTORY),
            (StockAdjustmentCategory.OTHER to StockAdjustmentDirection.INCREASE) to
                StockAdjustmentLedgerAccounts(SystemAccount.INVENTORY, SystemAccount.SHRINKAGE_AND_LOSSES)
        )

        expectedAccountsByCategoryAndDirection.forEach { (categoryAndDirection, expectedAccounts) ->
            assertEquals(
                expectedAccounts,
                StockAdjustmentCategoryRules.ledgerAccounts(categoryAndDirection.first, categoryAndDirection.second),
                categoryAndDirection.toString()
            )
        }
    }

    @Test
    fun `only other leaves the direction to the caller`() {
        assertEquals(
            setOf(StockAdjustmentCategory.OTHER),
            StockAdjustmentCategory.entries.filter { StockAdjustmentCategoryRules.fixedDirection(it) == null }.toSet()
        )
    }

    @Test
    fun `a unit cost is asked for exactly on increases, including the increase side of other`() {
        assertEquals(listOf(StockAdjustmentDirection.INCREASE), StockAdjustmentCategoryRules.unitCostDirections(StockAdjustmentCategory.OTHER))
        assertEquals(listOf(StockAdjustmentDirection.INCREASE), StockAdjustmentCategoryRules.unitCostDirections(StockAdjustmentCategory.FREE_GOODS))
        assertEquals(emptyList<StockAdjustmentDirection>(), StockAdjustmentCategoryRules.unitCostDirections(StockAdjustmentCategory.INTERNAL_USE))
    }
}
