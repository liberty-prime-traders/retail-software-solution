package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.util.UUID

class ExpenseLookup(
    private val expenseStore: ExpenseStore
) {

    fun requireExpense(expenseReference: String): ExpenseDto =
        expenseStore.findExpenseByReference(expenseReference) ?: throw RtsGenericException("Expense $expenseReference not found")

    fun requireExpenses(expenseReferences: Collection<String>): Map<String, ExpenseDto> {
        val expenseDtosByReference = expenseStore.findExpensesByReferences(expenseReferences).associateBy { it.referenceNumber }
        expenseReferences.firstOrNull { it !in expenseDtosByReference }
            ?.let { throw RtsGenericException("Expense $it not found") }
        return expenseDtosByReference
    }

    fun requireExpenseById(expenseId: UUID): ExpenseDto =
        expenseStore.findExpenseById(expenseId) ?: throw RtsGenericException("Expense $expenseId not found")

    fun requireNotVoided(expenseAggregate: ExpenseAggregate, expenseDto: ExpenseDto) {
        if (expenseAggregate.isVoided(expenseDto.id)) {
            throw RtsGenericException("Expense ${expenseDto.referenceNumber} has been voided")
        }
    }
}
