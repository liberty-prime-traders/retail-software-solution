package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.model.ViewNames

data class ExpenseSearchTables(
    val expense: String,
    val paymentState: String,
    val expenseVoid: String,
    val activeExpensePayment: String
) {
    companion object {
        val LOCATION = ExpenseSearchTables(
            TableNames.EXPENSE, TableNames.EXPENSE_PAYMENT_STATE, TableNames.EXPENSE_VOID, ViewNames.ACTIVE_EXPENSE_PAYMENT
        )

        val ORGANIZATION = ExpenseSearchTables(
            TableNames.ORG_EXPENSE, TableNames.ORG_EXPENSE_PAYMENT_STATE, TableNames.ORG_EXPENSE_VOID, ViewNames.ACTIVE_ORG_EXPENSE_PAYMENT
        )
    }
}
