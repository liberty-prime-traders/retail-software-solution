package me.ezra_home.retail_software_solution.cross_tier.expense.store

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentStateBase
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpensePaymentStateRepositoryBase
import me.ezra_home.retail_software_solution.util.business.PaymentStatusResolver
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal

object ExpensePaymentStateMaintainer {

    fun <STATE : ExpensePaymentStateBase> refresh(
        expensePaymentStateRepository: ExpensePaymentStateRepositoryBase<STATE>,
        expenseDtos: Collection<ExpenseDto>
    ) {
        if (expenseDtos.isEmpty()) return
        val expenseAmountsById = expenseDtos.associate { it.id to it.amount }
        val activePaidAmountsByExpenseId = expensePaymentStateRepository
            .sumActivePaidByExpenseId(expenseAmountsById.keys)
            .associate { it.expenseId to it.amountPaid }
        val expensePaymentStates = expensePaymentStateRepository.findByExpenseIdIn(expenseAmountsById.keys)
        val expenseIdsMissingState = expenseAmountsById.keys - expensePaymentStates.map { it.expenseId }.toSet()
        if (expenseIdsMissingState.isNotEmpty()) {
            throw RtsGenericException("Expense payment state is missing for expenses $expenseIdsMissingState")
        }
        expensePaymentStates.forEach { expensePaymentState ->
            val amountPaid = activePaidAmountsByExpenseId[expensePaymentState.expenseId] ?: BigDecimal.ZERO
            expensePaymentState.amountPaid = amountPaid
            expensePaymentState.paymentStatus =
                PaymentStatusResolver.resolve(amountPaid, expenseAmountsById.getValue(expensePaymentState.expenseId))
        }
        expensePaymentStateRepository.saveAll(expensePaymentStates)
    }
}
