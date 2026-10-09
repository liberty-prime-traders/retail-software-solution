package me.ezra_home.retail_software_solution.cross_tier.expense.response

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ExpenseSummaryResponse(
    val reference: String,
    val expenseTypeName: String,
    val payeeContactId: UUID,
    val payeeDisplayName: String,
    val amount: BigDecimal,
    val expenseDate: LocalDate,
    val description: String?,
    val sourceType: ExpenseSourceType,
    val sourceReference: String?,
    val batchReference: String,
    val batchDescription: String,
    val status: PaymentStatus,
    val amountPaid: BigDecimal,
    val balanceRemaining: BigDecimal,
    val voided: Boolean,
    val voidReason: String?,
    val createdOn: OffsetDateTime,
    val createdBy: String,
    val payments: List<ExpensePaymentResponse>
)

data class ExpensePaymentResponse(
    val reference: String,
    val amount: BigDecimal,
    val paymentMethodName: String,
    val providerReference: String?,
    val paymentDate: LocalDate,
    val voided: Boolean,
    val voidReason: String?,
    val voidedOn: OffsetDateTime?,
    val createdOn: OffsetDateTime
)
