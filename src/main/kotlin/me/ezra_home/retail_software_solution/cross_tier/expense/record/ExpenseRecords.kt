package me.ezra_home.retail_software_solution.cross_tier.expense.record

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ExpenseBatchRecord(
    val id: UUID,
    val referenceNumber: String,
    val description: String,
    val createdOn: OffsetDateTime,
    val createdById: UUID
)

data class ExpenseRecord(
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

data class ExpensePaymentRecord(
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

data class ExpenseVoidRecord(
    val id: UUID,
    val expenseId: UUID,
    val reason: String,
    val voidedOn: OffsetDateTime
)

data class ExpensePaymentVoidRecord(
    val id: UUID,
    val paymentId: UUID,
    val reason: String,
    val voidedOn: OffsetDateTime
)

data class SettledExpense(
    val expenseRecord: ExpenseRecord,
    val resolvedSettlement: ResolvedSettlement
)

data class ExpensePaymentStateRecord(
    val expenseId: UUID,
    val paymentStatus: PaymentStatus,
    val amountPaid: BigDecimal
)

data class ExpenseAggregate(
    val batches: List<ExpenseBatchRecord>,
    val expenses: List<ExpenseRecord>,
    val payments: List<ExpensePaymentRecord>,
    val paymentVoids: List<ExpensePaymentVoidRecord>,
    val expenseVoids: List<ExpenseVoidRecord>,
    val paymentStates: List<ExpensePaymentStateRecord> = emptyList()
) {
    fun forExpense(expenseId: UUID): ExpenseAggregate {
        val expensePayments = payments.filter { it.expenseId == expenseId }
        val expensePaymentIds = expensePayments.map { it.id }.toSet()
        return ExpenseAggregate(
            batches = batches,
            expenses = expenses.filter { it.id == expenseId },
            payments = expensePayments,
            paymentVoids = paymentVoids.filter { it.paymentId in expensePaymentIds },
            expenseVoids = expenseVoids.filter { it.expenseId == expenseId },
            paymentStates = paymentStates.filter { it.expenseId == expenseId }
        )
    }
}
