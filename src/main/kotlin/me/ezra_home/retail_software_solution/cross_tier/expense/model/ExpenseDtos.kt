package me.ezra_home.retail_software_solution.cross_tier.expense.model

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ExpenseBatchDto(
    val id: UUID,
    val referenceNumber: String,
    val description: String,
    val createdOn: OffsetDateTime,
    val createdById: UUID
)

data class ExpenseDto(
    val id: UUID,
    val referenceNumber: String,
    val expenseTypeId: UUID,
    val expenseAccountCode: String,
    val payeeContactId: UUID,
    val amount: BigDecimal,
    val expenseDate: LocalDate,
    val description: String?,
    val sourceType: ExpenseSourceType,
    val sourceReference: String?,
    val batchId: UUID,
    val createdOn: OffsetDateTime,
    val createdById: UUID
)

data class ExpensePaymentDto(
    val id: UUID,
    val expenseId: UUID,
    val referenceNumber: String,
    val paymentMethodId: UUID,
    val paymentMethodAccountCode: String,
    val amount: BigDecimal,
    val providerReference: String?,
    val paymentDate: OffsetDateTime,
    val createdOn: OffsetDateTime
)

data class ExpenseVoidDto(
    val id: UUID,
    val expenseId: UUID,
    val reason: String,
    val voidedOn: OffsetDateTime
)

data class ExpensePaymentVoidDto(
    val id: UUID,
    val paymentId: UUID,
    val reason: String,
    val voidedOn: OffsetDateTime
)

data class SettledExpense(
    val expenseDto: ExpenseDto,
    val resolvedSettlement: ResolvedSettlement
)

data class ExpensePaymentStateDto(
    val expenseId: UUID,
    val paymentStatus: PaymentStatus,
    val amountPaid: BigDecimal
)

data class ExpenseAggregate(
    val batches: List<ExpenseBatchDto>,
    val expenses: List<ExpenseDto>,
    val payments: List<ExpensePaymentDto>,
    val paymentVoids: List<ExpensePaymentVoidDto>,
    val expenseVoids: List<ExpenseVoidDto>,
    val paymentStates: List<ExpensePaymentStateDto> = emptyList()
) {
    private val voidedPaymentIds: Set<UUID> = paymentVoids.map { it.paymentId }.toSet()
    private val expensesById: Map<UUID, List<ExpenseDto>> = expenses.groupBy { it.id }
    private val paymentsByExpenseId: Map<UUID, List<ExpensePaymentDto>> = payments.groupBy { it.expenseId }
    private val paymentVoidsByPaymentId: Map<UUID, List<ExpensePaymentVoidDto>> = paymentVoids.groupBy { it.paymentId }
    private val expenseVoidsByExpenseId: Map<UUID, List<ExpenseVoidDto>> = expenseVoids.groupBy { it.expenseId }
    private val paymentStatesByExpenseId: Map<UUID, List<ExpensePaymentStateDto>> = paymentStates.groupBy { it.expenseId }

    fun isVoided(expenseId: UUID): Boolean = expenseId in expenseVoidsByExpenseId

    fun hasActivePayments(expenseId: UUID): Boolean = activePaymentsOf(expenseId).isNotEmpty()

    fun activePaidAmount(expenseId: UUID): BigDecimal = activePaymentsOf(expenseId).sumOf { it.amount }

    private fun activePaymentsOf(expenseId: UUID): List<ExpensePaymentDto> =
        paymentsByExpenseId[expenseId].orEmpty().filter { it.id !in voidedPaymentIds }

    fun forExpense(expenseId: UUID): ExpenseAggregate {
        val expensePayments = paymentsByExpenseId[expenseId].orEmpty()
        return ExpenseAggregate(
            batches = batches,
            expenses = expensesById[expenseId].orEmpty(),
            payments = expensePayments,
            paymentVoids = expensePayments.flatMap { paymentVoidsByPaymentId[it.id].orEmpty() },
            expenseVoids = expenseVoidsByExpenseId[expenseId].orEmpty(),
            paymentStates = paymentStatesByExpenseId[expenseId].orEmpty()
        )
    }
}
