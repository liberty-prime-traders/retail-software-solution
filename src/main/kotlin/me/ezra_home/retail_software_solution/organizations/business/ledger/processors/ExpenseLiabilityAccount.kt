package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount

object ExpenseLiabilityAccount {

    fun forExpenseAccount(expenseAccountCode: String): String =
        if (expenseAccountCode == SystemAccount.WAGES_EXPENSE.code) {
            SystemAccount.WAGES_PAYABLE.code
        } else {
            SystemAccount.TRADE_PAYABLES.code
        }
}
