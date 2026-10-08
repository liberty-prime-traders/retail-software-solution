package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExpenseLookup {

    fun requireExpense(expenseStore: ExpenseStore, expenseReference: String): ExpenseRecord =
        expenseStore.findExpenseByReference(expenseReference) ?: throw RtsGenericException("Expense $expenseReference not found")

    fun requireExpenses(expenseStore: ExpenseStore, expenseReferences: Collection<String>): Map<String, ExpenseRecord> {
        val expenseRecordsByReference = expenseStore.findExpensesByReferences(expenseReferences).associateBy { it.referenceNumber }
        expenseReferences.firstOrNull { it !in expenseRecordsByReference }
            ?.let { throw RtsGenericException("Expense $it not found") }
        return expenseRecordsByReference
    }

    fun requireExpenseById(expenseStore: ExpenseStore, expenseId: UUID): ExpenseRecord =
        expenseStore.findExpenseById(expenseId) ?: throw RtsGenericException("Expense $expenseId not found")

    fun requireNotVoided(expenseAggregate: ExpenseAggregate, expenseRecord: ExpenseRecord) {
        if (expenseAggregate.expenseVoids.isNotEmpty()) {
            throw RtsGenericException("Expense ${expenseRecord.referenceNumber} has been voided")
        }
    }
}
