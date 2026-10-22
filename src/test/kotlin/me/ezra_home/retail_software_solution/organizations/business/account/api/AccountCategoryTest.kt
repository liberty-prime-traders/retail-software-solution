package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountCategoryTest {

    @Test
    fun `a category's increasing effect follows its root account type`() {
        assertEquals(BalanceEffect.ADDS, AccountCategory.ASSET.balanceEffectWhenIncreasing())
        assertEquals(BalanceEffect.SUBTRACTS, AccountCategory.LIABILITY.balanceEffectWhenIncreasing())
    }
}
