package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.util.enums.HasCode
import me.ezra_home.retail_software_solution.util.ui_models.BalanceSignal

enum class AccountType(override val code: String, val normalBalance: EntryType) : HasCode {
    ASSET("A", EntryType.DEBIT),
    ASSET_CONTRA("AC", EntryType.CREDIT),

    EXPENSE("X", EntryType.DEBIT),

    LIABILITY("L", EntryType.CREDIT),
    LIABILITY_CONTRA("LC", EntryType.DEBIT),

    EQUITY("E", EntryType.CREDIT),
    EQUITY_CONTRA("EC", EntryType.DEBIT),

    REVENUE("R", EntryType.CREDIT),
    REVENUE_CONTRA("RC", EntryType.DEBIT);

    fun canBeRoot() = this in setOf(ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE)
    fun isClosingType() = this in setOf(REVENUE, REVENUE_CONTRA, EXPENSE)

    fun balanceSignal() = when (this) {
        ASSET, REVENUE, EQUITY, LIABILITY_CONTRA -> BalanceSignal.MONEY_IN
        LIABILITY, EXPENSE, ASSET_CONTRA, EQUITY_CONTRA, REVENUE_CONTRA -> BalanceSignal.MONEY_OUT
    }
}
