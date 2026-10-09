package me.ezra_home.retail_software_solution.cross_tier.expense.store

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseBatchDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentVoidDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseVoidDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpense
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpenseBatch
import java.util.UUID

interface ExpenseStore {

    fun createBatch(newExpenseBatch: NewExpenseBatch): ExpenseBatchDto

    fun findBatchBySource(sourceType: ExpenseSourceType, sourceReference: String): ExpenseBatchDto?

    fun saveExpense(newExpense: NewExpense): ExpenseDto

    fun savePayments(expensePaymentDrafts: List<ExpensePaymentDraft>): List<ExpensePaymentDto>

    fun refreshPaymentStates(expenseDtos: Collection<ExpenseDto>)

    fun saveExpenseVoid(expenseId: UUID, reason: String): ExpenseVoidDto

    fun savePaymentVoid(paymentId: UUID, reason: String): ExpensePaymentVoidDto

    fun findExpenseById(expenseId: UUID): ExpenseDto?

    fun findExpenseByReference(referenceNumber: String): ExpenseDto?

    fun findExpensesByReferences(referenceNumbers: Collection<String>): List<ExpenseDto>

    fun findPaymentById(paymentId: UUID): ExpensePaymentDto?

    fun findPaymentByReference(referenceNumber: String): ExpensePaymentDto?

    fun findExpenseVoidById(expenseVoidId: UUID): ExpenseVoidDto?

    fun findPaymentVoidById(paymentVoidId: UUID): ExpensePaymentVoidDto?

    fun loadForBatch(batchId: UUID): ExpenseAggregate

    fun loadForExpenses(expenseIds: Collection<UUID>): ExpenseAggregate

    fun loadRecent(limit: Int): ExpenseAggregate
}
