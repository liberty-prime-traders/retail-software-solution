package me.ezra_home.retail_software_solution.cross_tier.expense.store

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBatchBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentStateBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentVoidBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseVoidBase
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseBatchDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentStateDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentVoidDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseVoidDto

object ExpenseDtoMapper {

    fun toDto(expenseBatchBase: ExpenseBatchBase) = ExpenseBatchDto(
        expenseBatchBase.id!!, expenseBatchBase.requiredReference(), expenseBatchBase.description,
        expenseBatchBase.requiredCreatedOn(), expenseBatchBase.requiredCreatedById()
    )

    fun toDto(expenseBase: ExpenseBase) = ExpenseDto(
        expenseBase.id!!, expenseBase.requiredReference(), expenseBase.expenseTypeId, expenseBase.expenseAccountCode,
        expenseBase.payeeContactId, expenseBase.amount, expenseBase.expenseDate, expenseBase.description, expenseBase.sourceType,
        expenseBase.sourceReference, expenseBase.batchId, expenseBase.requiredCreatedOn(), expenseBase.requiredCreatedById()
    )

    fun toDto(expensePaymentBase: ExpensePaymentBase) = ExpensePaymentDto(
        expensePaymentBase.id!!, expensePaymentBase.expenseId, expensePaymentBase.requiredReference(),
        expensePaymentBase.paymentMethodId, expensePaymentBase.paymentMethodAccountCode, expensePaymentBase.amount,
        expensePaymentBase.providerReference, expensePaymentBase.paymentDate, expensePaymentBase.requiredCreatedOn()
    )

    fun toDto(expensePaymentVoidBase: ExpensePaymentVoidBase) = ExpensePaymentVoidDto(
        expensePaymentVoidBase.id!!, expensePaymentVoidBase.expensePaymentId, expensePaymentVoidBase.reason,
        expensePaymentVoidBase.requiredCreatedOn()
    )

    fun toDto(expensePaymentStateBase: ExpensePaymentStateBase) = ExpensePaymentStateDto(
        expensePaymentStateBase.expenseId, expensePaymentStateBase.paymentStatus, expensePaymentStateBase.amountPaid
    )

    fun toDto(expenseVoidBase: ExpenseVoidBase) = ExpenseVoidDto(
        expenseVoidBase.id!!, expenseVoidBase.expenseId, expenseVoidBase.reason, expenseVoidBase.requiredCreatedOn()
    )
}
