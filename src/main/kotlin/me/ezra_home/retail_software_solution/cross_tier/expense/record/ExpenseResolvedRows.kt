package me.ezra_home.retail_software_solution.cross_tier.expense.record

import me.ezra_home.retail_software_solution.cross_tier.expense.api.PaymentInstruction
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeDto
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ExpenseRowCommand(
    val expenseTypeId: UUID,
    val payeeContactId: UUID,
    val amount: BigDecimal,
    val description: String?,
    val expenseDateOverride: LocalDate?,
    val settlement: PaymentInstruction?
)

data class ResolvedSettlement(
    val paymentMethodId: UUID,
    val paymentMethodAccountCode: String,
    val providerReference: String?,
    val paymentDate: LocalDate
)

data class ExpensePaymentDraft(
    val expenseId: UUID,
    val amount: BigDecimal,
    val resolvedSettlement: ResolvedSettlement
)

data class ResolvedExpenseRow(
    val expenseType: ExpenseTypeDto,
    val payee: ContactDto,
    val amount: BigDecimal,
    val description: String?,
    val expenseDate: LocalDate,
    val settlement: ResolvedSettlement?
)
