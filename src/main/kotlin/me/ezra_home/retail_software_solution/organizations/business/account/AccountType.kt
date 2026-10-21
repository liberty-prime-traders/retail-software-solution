package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountCategory
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.util.enums.HasCode
import me.ezra_home.retail_software_solution.util.ui_models.BalanceSignal

enum class AccountType(override val code: String, val normalBalance: EntryType) : HasCode {
    ASSET("A", EntryType.DEBIT),
    ASSET_CONTRA("AC", EntryType.CREDIT),

    EXPENSE("X", EntryType.DEBIT),
    EXPENSE_CONTRA("XC", EntryType.CREDIT),

    LIABILITY("L", EntryType.CREDIT),
    LIABILITY_CONTRA("LC", EntryType.DEBIT),

    EQUITY("E", EntryType.CREDIT),
    EQUITY_CONTRA("EC", EntryType.DEBIT),

    REVENUE("R", EntryType.CREDIT),
    REVENUE_CONTRA("RC", EntryType.DEBIT);

    fun isContraOf(baseType: AccountType) = when (this) {
        ASSET_CONTRA -> baseType == ASSET
        LIABILITY_CONTRA -> baseType == LIABILITY
        EQUITY_CONTRA -> baseType == EQUITY
        EXPENSE_CONTRA -> baseType == EXPENSE
        REVENUE_CONTRA -> baseType == REVENUE
        else -> false
    }

    fun category() = when (this) {
        ASSET, ASSET_CONTRA -> AccountCategory.ASSET
        LIABILITY, LIABILITY_CONTRA -> AccountCategory.LIABILITY
        EQUITY, EQUITY_CONTRA -> AccountCategory.EQUITY
        REVENUE, REVENUE_CONTRA -> AccountCategory.REVENUE
        EXPENSE, EXPENSE_CONTRA -> AccountCategory.EXPENSE
    }

    fun isContra() = this in CONTRA_ACCOUNTS
    fun canBeRoot() = this in ROOT_ACCOUNTS
    fun isClosingType() = this in CLOSING_ACCOUNTS

    fun balanceSignal() = when (this) {
        ASSET, REVENUE, EQUITY, LIABILITY_CONTRA, EXPENSE_CONTRA -> BalanceSignal.MONEY_IN
        LIABILITY, EXPENSE, ASSET_CONTRA, EQUITY_CONTRA, REVENUE_CONTRA -> BalanceSignal.MONEY_OUT
    }

    companion object {
        val CONTRA_ACCOUNTS = setOf(ASSET_CONTRA, LIABILITY_CONTRA, EQUITY_CONTRA, REVENUE_CONTRA, EXPENSE_CONTRA)
        val ROOT_ACCOUNTS = setOf(ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE)
        val CLOSING_ACCOUNTS = setOf(REVENUE, REVENUE_CONTRA, EXPENSE, EXPENSE_CONTRA)
    }
}
