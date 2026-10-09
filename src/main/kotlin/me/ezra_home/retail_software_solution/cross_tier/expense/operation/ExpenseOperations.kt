package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseTier
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseBatchDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseSource
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseSubmission
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpense
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpenseBatch
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ResolvedExpenseRow
import me.ezra_home.retail_software_solution.cross_tier.expense.model.SettledExpense
import me.ezra_home.retail_software_solution.cross_tier.expense.model.SourceDocument
import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpenseVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.request.StandaloneExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

class ExpenseOperations(
    private val expenseTier: ExpenseTier,
    private val expenseEvents: ExpenseEvents,
    private val expenseRowResolver: ExpenseRowResolver,
    private val expenseResponseBuilder: ExpenseResponseBuilder,
    private val expenseLookup: ExpenseLookup,
    private val expensePaymentOperations: ExpensePaymentOperations
) {

    private val expenseStore = expenseTier.expenseStore

    fun createStandalone(standaloneExpenseBatchRequest: StandaloneExpenseBatchRequest): List<ExpenseSummaryResponse> {
        val batchDescription = StringUtils.getValueOrException(
            standaloneExpenseBatchRequest.description, "Batch description is required"
        )
        return createBatch(
            ExpenseSourceType.ADHOC,
            batchDescription,
            ExpenseSubmission(standaloneExpenseBatchRequest.expenseDate, standaloneExpenseBatchRequest.rows.map { it.toRowCommand() })
        )
    }

    fun createBatch(
        sourceType: ExpenseSourceType,
        description: String,
        expenseSubmission: ExpenseSubmission
    ): List<ExpenseSummaryResponse> {
        val resolvedExpenseRows = expenseRowResolver.resolve(sourceType, expenseSubmission)
        val expenseSource = ExpenseSource(sourceType, null)
        val expenseBatchDto = expenseStore.createBatch(NewExpenseBatch(description, expenseSource))
        return expenseResponseBuilder.buildSummaries(writeRows(expenseBatchDto, expenseSource, resolvedExpenseRows))
    }

    fun createForSourceDocument(sourceDocument: SourceDocument, expenseSubmission: ExpenseSubmission): List<ExpenseSummaryResponse> {
        val resolvedExpenseRows = expenseRowResolver.resolve(sourceDocument.type, expenseSubmission)
        expenseTier.lockSourceDocument(sourceDocument.id)
        val expenseBatchDto = expenseStore.findBatchBySource(sourceDocument.type, sourceDocument.reference)
            ?: expenseStore.createBatch(
                NewExpenseBatch("${sourceDocument.type.batchDescriptionLabel()} ${sourceDocument.reference}", sourceDocument.source)
            )
        return expenseResponseBuilder.buildSummaries(writeRows(expenseBatchDto, sourceDocument.source, resolvedExpenseRows))
    }

    fun voidExpense(expenseVoidRequest: ExpenseVoidRequest): ExpenseSummaryResponse {
        val voidReason = StringUtils.getValueOrException(expenseVoidRequest.reason, "A void reason is required")
        expenseRowResolver.requireOpenPeriodToday()
        val expenseDto = expenseLookup.requireExpense(expenseVoidRequest.expenseReference)
        expenseTier.lockExpense(expenseDto.id)
        val expenseAggregate = expenseStore.loadForExpenses(listOf(expenseDto.id))
        expenseLookup.requireNotVoided(expenseAggregate, expenseDto)
        if (expenseAggregate.hasActivePayments(expenseDto.id)) {
            throw RtsGenericException("Void every payment on expense ${expenseDto.referenceNumber} before voiding it")
        }
        val expenseVoidDto = expenseStore.saveExpenseVoid(expenseDto.id, voidReason)
        expenseEvents.publishVoided(expenseVoidDto, expenseDto)
        return expenseResponseBuilder.buildSummary(
            expenseAggregate.copy(expenseVoids = expenseAggregate.expenseVoids + expenseVoidDto), expenseDto.id
        )
    }

    private fun writeRows(
        expenseBatchDto: ExpenseBatchDto,
        expenseSource: ExpenseSource,
        resolvedExpenseRows: List<ResolvedExpenseRow>
    ): ExpenseAggregate {
        val settledExpenses = ArrayList<SettledExpense>()
        val writtenExpenseIds = resolvedExpenseRows.map { resolvedExpenseRow ->
            val expenseDto = expenseStore.saveExpense(NewExpense(expenseBatchDto.id, expenseSource, resolvedExpenseRow))
            expenseEvents.publishRecorded(expenseDto)
            resolvedExpenseRow.settlement?.let { settledExpenses.add(SettledExpense(expenseDto, it)) }
            expenseDto.id
        }
        expensePaymentOperations.settleNewExpenses(settledExpenses)
        val writtenAggregate = expenseStore.loadForExpenses(writtenExpenseIds)
        val writtenOrderByExpenseId = writtenExpenseIds.withIndex().associate { (index, expenseId) -> expenseId to index }
        return writtenAggregate.copy(expenses = writtenAggregate.expenses.sortedBy { writtenOrderByExpenseId.getValue(it.id) })
    }
}
