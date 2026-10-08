package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.util.model.TableNames

data class ExpenseSearchTables(
    val expense: String,
    val paymentState: String,
    val expenseVoid: String,
    val expensePayment: String,
    val expensePaymentVoid: String
) {
    companion object {
        val LOCATION = ExpenseSearchTables(
            TableNames.EXPENSE, TableNames.EXPENSE_PAYMENT_STATE, TableNames.EXPENSE_VOID,
            TableNames.EXPENSE_PAYMENT, TableNames.EXPENSE_PAYMENT_VOID
        )

        val ORGANIZATION = ExpenseSearchTables(
            TableNames.ORG_EXPENSE, TableNames.ORG_EXPENSE_PAYMENT_STATE, TableNames.ORG_EXPENSE_VOID,
            TableNames.ORG_EXPENSE_PAYMENT, TableNames.ORG_EXPENSE_PAYMENT_VOID
        )
    }
}
