package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountType
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect

enum class AccountCategory {
    ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE;

    fun balanceEffectWhenIncreasing(): BalanceEffect = AccountType.rootOf(this).balanceEffect()
}
