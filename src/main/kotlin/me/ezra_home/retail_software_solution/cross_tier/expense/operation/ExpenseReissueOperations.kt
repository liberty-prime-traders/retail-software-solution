package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseTier
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.util.UUID

class ExpenseReissueOperations(
    expenseTier: ExpenseTier,
    private val expenseEvents: ExpenseEvents,
    private val expenseLookup: ExpenseLookup
) {

    private val expenseStore = expenseTier.expenseStore

    fun reissueRecorded(expenseId: UUID) {
        expenseEvents.publishRecorded(expenseLookup.requireExpenseById(expenseId))
    }

    fun reissueVoided(expenseVoidId: UUID) {
        val expenseVoidDto = expenseStore.findExpenseVoidById(expenseVoidId)
            ?: throw RtsGenericException("Expense void $expenseVoidId not found")
        expenseEvents.publishVoided(expenseVoidDto, expenseLookup.requireExpenseById(expenseVoidDto.expenseId))
    }

    fun reissuePaymentRecorded(paymentId: UUID) {
        val expensePaymentDto = requirePaymentById(paymentId)
        val expenseDto = expenseLookup.requireExpenseById(expensePaymentDto.expenseId)
        expenseEvents.publishPaymentRecorded(expensePaymentDto, expenseDto)
    }

    fun reissuePaymentVoided(paymentVoidId: UUID) {
        val expensePaymentVoidDto = expenseStore.findPaymentVoidById(paymentVoidId)
            ?: throw RtsGenericException("Expense payment void $paymentVoidId not found")
        val expensePaymentDto = requirePaymentById(expensePaymentVoidDto.paymentId)
        val expenseDto = expenseLookup.requireExpenseById(expensePaymentDto.expenseId)
        expenseEvents.publishPaymentVoided(expensePaymentVoidDto, expensePaymentDto, expenseDto)
    }

    private fun requirePaymentById(paymentId: UUID): ExpensePaymentDto =
        expenseStore.findPaymentById(paymentId) ?: throw RtsGenericException("Expense payment $paymentId not found")
}
