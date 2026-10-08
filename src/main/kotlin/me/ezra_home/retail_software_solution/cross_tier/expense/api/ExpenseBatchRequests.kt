package me.ezra_home.retail_software_solution.cross_tier.expense.api

import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRowCommand
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class StandaloneExpenseBatchRequest(
    val description: String,
    val expenseDate: LocalDate,
    val rows: List<StandaloneExpenseRowRequest>
)

data class StandaloneExpenseRowRequest(
    val payeeContactId: UUID,
    val expenseTypeId: UUID,
    val amount: BigDecimal,
    val description: String? = null,
    val expenseDateOverride: LocalDate? = null,
    val settlement: PaymentInstruction? = null
) {
    fun toRowCommand() = ExpenseRowCommand(expenseTypeId, payeeContactId, amount, description, expenseDateOverride, settlement)
}

data class PaymentInstruction(
    val paymentMethodId: UUID,
    val paymentReference: String? = null,
    val paymentDate: LocalDate? = null
)

data class WageExpenseBatchRequest(
    val expenseDate: LocalDate,
    val rows: List<WageExpenseRowRequest>
)

data class WageExpenseRowRequest(
    val employeeContactId: UUID,
    val amount: BigDecimal,
    val expenseDateOverride: LocalDate? = null,
    val settlement: PaymentInstruction? = null
)

data class PurchaseExpenseBatchRequest(
    val purchaseReference: String,
    val expenseDate: LocalDate,
    val rows: List<PurchaseExpenseRowRequest>
)

data class PurchaseExpenseRowRequest(
    val payeeContactId: UUID? = null,
    val expenseTypeId: UUID,
    val amount: BigDecimal,
    val description: String? = null,
    val expenseDateOverride: LocalDate? = null,
    val settlement: PaymentInstruction? = null
)

data class SaleExpenseBatchRequest(
    val saleReference: String,
    val expenseDate: LocalDate,
    val rows: List<RequiredPayeeExpenseRowRequest>
)

data class StockTransferExpenseBatchRequest(
    val transferReference: String,
    val expenseDate: LocalDate,
    val rows: List<RequiredPayeeExpenseRowRequest>
)

data class RequiredPayeeExpenseRowRequest(
    val payeeContactId: UUID,
    val expenseTypeId: UUID,
    val amount: BigDecimal,
    val description: String? = null,
    val expenseDateOverride: LocalDate? = null,
    val settlement: PaymentInstruction? = null
) {
    fun toRowCommand() = ExpenseRowCommand(expenseTypeId, payeeContactId, amount, description, expenseDateOverride, settlement)
}
