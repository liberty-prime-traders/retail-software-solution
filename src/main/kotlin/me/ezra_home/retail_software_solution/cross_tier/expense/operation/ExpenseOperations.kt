package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.StandaloneExpenseBatchRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseBatchRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRowCommand
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseVoidRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedExpenseRow
import me.ezra_home.retail_software_solution.cross_tier.expense.record.SettledExpense
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseVoidedEvent
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Component
class ExpenseOperations(
    private val expenseRowResolver: ExpenseRowResolver,
    private val expenseResponseBuilder: ExpenseResponseBuilder,
    private val expenseLookup: ExpenseLookup,
    private val expensePaymentOperations: ExpensePaymentOperations,
    private val fiscalPeriodService: FiscalPeriodService,
    private val eventPublisher: ApplicationEventPublisher
) {

    fun createStandalone(
        expenseStore: ExpenseStore,
        standaloneExpenseBatchRequest: StandaloneExpenseBatchRequest
    ): List<ExpenseSummaryResponse> {
        val batchDescription = StringUtils.getValueOrException(
            standaloneExpenseBatchRequest.description, "Batch description is required"
        )
        return createBatch(
            expenseStore,
            ExpenseSourceType.ADHOC,
            batchDescription,
            standaloneExpenseBatchRequest.expenseDate,
            standaloneExpenseBatchRequest.rows.map { it.toRowCommand() }
        )
    }

    fun createBatch(
        expenseStore: ExpenseStore,
        sourceType: ExpenseSourceType,
        description: String,
        expenseDate: LocalDate,
        rowCommands: List<ExpenseRowCommand>
    ): List<ExpenseSummaryResponse> {
        val resolvedExpenseRows = expenseRowResolver.resolve(sourceType, expenseDate, rowCommands)
        val expenseBatchRecord = expenseStore.createBatch(description, sourceType, null)
        val writtenAggregate = writeRows(expenseStore, expenseBatchRecord, sourceType, null, resolvedExpenseRows)
        return summariesOf(writtenAggregate)
    }

    fun createForSourceDocument(
        expenseStore: ExpenseStore,
        sourceType: ExpenseSourceType,
        sourceReference: String,
        sourceDocumentId: UUID,
        expenseDate: LocalDate,
        rowCommands: List<ExpenseRowCommand>
    ): List<ExpenseSummaryResponse> {
        val resolvedExpenseRows = expenseRowResolver.resolve(sourceType, expenseDate, rowCommands)
        expenseStore.lockSourceDocument(sourceDocumentId)
        val expenseBatchRecord = expenseStore.findBatchBySource(sourceType, sourceReference)
            ?: expenseStore.createBatch("${sourceType.batchDescriptionLabel()} $sourceReference", sourceType, sourceReference)
        return summariesOf(writeRows(expenseStore, expenseBatchRecord, sourceType, sourceReference, resolvedExpenseRows))
    }

    fun voidExpense(expenseStore: ExpenseStore, expenseVoidRequest: ExpenseVoidRequest): ExpenseSummaryResponse {
        val voidReason = StringUtils.getValueOrException(expenseVoidRequest.reason, "A void reason is required")
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
        val expenseRecord = expenseLookup.requireExpense(expenseStore, expenseVoidRequest.expenseReference)
        expenseStore.lockExpense(expenseRecord.id)
        val expenseAggregate = expenseStore.loadForExpenses(listOf(expenseRecord.id))
        expenseLookup.requireNotVoided(expenseAggregate, expenseRecord)
        if (expenseAggregate.payments.size > expenseAggregate.paymentVoids.size) {
            throw RtsGenericException("Void every payment on expense ${expenseRecord.referenceNumber} before voiding it")
        }
        val expenseVoidRecord = expenseStore.saveExpenseVoid(expenseRecord.id, voidReason)
        publishVoided(expenseStore, expenseVoidRecord, expenseRecord)
        return expenseResponseBuilder.buildSummary(
            expenseAggregate.copy(expenseVoids = expenseAggregate.expenseVoids + expenseVoidRecord), expenseRecord.id
        )
    }

    fun getExpense(expenseStore: ExpenseStore, expenseReference: String): ExpenseSummaryResponse {
        val expenseRecord = expenseLookup.requireExpense(expenseStore, expenseReference)
        return expenseResponseBuilder.buildSummary(expenseStore.loadForExpenses(listOf(expenseRecord.id)), expenseRecord.id)
    }

    fun getBySource(expenseStore: ExpenseStore, sourceType: ExpenseSourceType, sourceReference: String): List<ExpenseSummaryResponse> {
        val expenseBatchRecord = expenseStore.findBatchBySource(sourceType, sourceReference) ?: return emptyList()
        return summariesOf(expenseStore.loadForBatch(expenseBatchRecord.id)).sortedBy { it.createdOn }
    }

    fun getRecent(expenseStore: ExpenseStore, limit: Int): List<ExpenseSummaryResponse> {
        if (limit !in 1..MAXIMUM_RECENT_EXPENSES) throw RtsGenericException("Limit must be between 1 and $MAXIMUM_RECENT_EXPENSES")
        return summariesOf(expenseStore.loadRecent(limit))
    }

    fun reissueRecorded(expenseStore: ExpenseStore, expenseId: UUID) {
        val expenseRecord = expenseLookup.requireExpenseById(expenseStore, expenseId)
        publishRecorded(expenseStore, expenseRecord)
    }

    fun reissueVoided(expenseStore: ExpenseStore, expenseVoidId: UUID) {
        val expenseVoidRecord = expenseStore.findExpenseVoidById(expenseVoidId)
            ?: throw RtsGenericException("Expense void $expenseVoidId not found")
        val expenseRecord = expenseLookup.requireExpenseById(expenseStore, expenseVoidRecord.expenseId)
        publishVoided(expenseStore, expenseVoidRecord, expenseRecord)
    }

    private fun writeRows(
        expenseStore: ExpenseStore,
        expenseBatchRecord: ExpenseBatchRecord,
        sourceType: ExpenseSourceType,
        sourceReference: String?,
        resolvedExpenseRows: List<ResolvedExpenseRow>
    ): ExpenseAggregate {
        val settledExpenses = ArrayList<SettledExpense>()
        val writtenExpenseIds = resolvedExpenseRows.map { resolvedExpenseRow ->
            val expenseRecord = expenseStore.saveExpense(expenseBatchRecord.id, sourceType, sourceReference, resolvedExpenseRow)
            publishRecorded(expenseStore, expenseRecord)
            resolvedExpenseRow.settlement?.let { settledExpenses.add(SettledExpense(expenseRecord, it)) }
            expenseRecord.id
        }
        expensePaymentOperations.settleNewExpenses(expenseStore, settledExpenses)
        val writtenAggregate = expenseStore.loadForExpenses(writtenExpenseIds)
        val writtenOrderByExpenseId = writtenExpenseIds.withIndex().associate { (index, expenseId) -> expenseId to index }
        return writtenAggregate.copy(expenses = writtenAggregate.expenses.sortedBy { writtenOrderByExpenseId.getValue(it.id) })
    }

    private fun summariesOf(expenseAggregate: ExpenseAggregate): List<ExpenseSummaryResponse> =
        if (expenseAggregate.expenses.isEmpty()) emptyList() else expenseResponseBuilder.buildSummaries(expenseAggregate)

    private fun publishRecorded(expenseStore: ExpenseStore, expenseRecord: ExpenseRecord) {
        eventPublisher.publishEvent(
            ExpenseRecordedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = expenseStore.sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                expenseId = expenseRecord.id,
                expenseReferenceNumber = expenseRecord.referenceNumber,
                expenseAccountCode = expenseRecord.expenseAccountCode,
                payeeContactId = expenseRecord.payeeContactId,
                amount = expenseRecord.amount,
                expenseDate = expenseRecord.expenseDate
            )
        )
    }

    private fun publishVoided(expenseStore: ExpenseStore, expenseVoidRecord: ExpenseVoidRecord, expenseRecord: ExpenseRecord) {
        eventPublisher.publishEvent(
            ExpenseVoidedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = expenseStore.sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                voidId = expenseVoidRecord.id,
                expenseId = expenseRecord.id,
                expenseReferenceNumber = expenseRecord.referenceNumber,
                expenseAccountCode = expenseRecord.expenseAccountCode,
                payeeContactId = expenseRecord.payeeContactId,
                amount = expenseRecord.amount,
                voidedOn = DateTimes.Local.atOrganizationZone(expenseVoidRecord.voidedOn)
            )
        )
    }

    companion object {
        private const val MAXIMUM_RECENT_EXPENSES = 200
    }
}
