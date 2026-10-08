package me.ezra_home.retail_software_solution.cross_tier.expense.store

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseBatchRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentVoidRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseVoidRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedExpenseRow
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import java.util.UUID

interface ExpenseStore {

    fun sourceContext(): EventSourceContext

    fun lockExpense(expenseId: UUID)

    fun lockSourceDocument(sourceDocumentId: UUID)

    fun createBatch(description: String, sourceType: ExpenseSourceType, sourceReference: String?): ExpenseBatchRecord

    fun findBatchBySource(sourceType: ExpenseSourceType, sourceReference: String): ExpenseBatchRecord?

    fun saveExpense(
        batchId: UUID,
        sourceType: ExpenseSourceType,
        sourceReference: String?,
        resolvedExpenseRow: ResolvedExpenseRow
    ): ExpenseRecord

    fun savePayments(expensePaymentDrafts: List<ExpensePaymentDraft>): List<ExpensePaymentRecord>

    fun refreshPaymentStates(expenseRecords: Collection<ExpenseRecord>)

    fun saveExpenseVoid(expenseId: UUID, reason: String): ExpenseVoidRecord

    fun savePaymentVoid(paymentId: UUID, reason: String): ExpensePaymentVoidRecord

    fun findExpenseById(expenseId: UUID): ExpenseRecord?

    fun findExpenseByReference(referenceNumber: String): ExpenseRecord?

    fun findExpensesByReferences(referenceNumbers: Collection<String>): List<ExpenseRecord>

    fun findPaymentById(paymentId: UUID): ExpensePaymentRecord?

    fun findPaymentByReference(referenceNumber: String): ExpensePaymentRecord?

    fun findExpenseVoidById(expenseVoidId: UUID): ExpenseVoidRecord?

    fun findPaymentVoidById(paymentVoidId: UUID): ExpensePaymentVoidRecord?

    fun loadForBatch(batchId: UUID): ExpenseAggregate

    fun loadForExpenses(expenseIds: Collection<UUID>): ExpenseAggregate

    fun loadRecent(limit: Int): ExpenseAggregate
}
