package me.ezra_home.retail_software_solution.cross_tier.expense.api

import java.math.BigDecimal

data class ExpensePaymentCreateRequest(
    val expenseReference: String,
    val settlement: PaymentInstruction,
    val amount: BigDecimal
)

data class ExpenseVoidRequest(
    val expenseReference: String,
    val reason: String
)

data class ExpensePaymentVoidRequest(
    val paymentReference: String,
    val reason: String
)
