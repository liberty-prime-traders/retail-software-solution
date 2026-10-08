package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpensePaymentCreateRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpensePaymentVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentVoidRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedSettlement
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentVoidedEvent
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Component
class ExpensePaymentOperations(
    private val expenseRowResolver: ExpenseRowResolver,
    private val expenseResponseBuilder: ExpenseResponseBuilder,
    private val expenseLookup: ExpenseLookup,
    private val fiscalPeriodService: FiscalPeriodService,
    private val eventPublisher: ApplicationEventPublisher
) {

    fun recordPayments(
        expenseStore: ExpenseStore,
        expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>
    ): List<ExpenseSummaryResponse> {
        requireWithinRequestLimits(expensePaymentCreateRequests)
        val resolvedPaymentRequestsByExpenseReference = resolveByExpenseReference(expensePaymentCreateRequests)
        val expenseRecordsByReference = expenseLookup.requireExpenses(expenseStore, resolvedPaymentRequestsByExpenseReference.keys.toList())
        val expenseAggregatesById = lockAndLoad(expenseStore, expenseRecordsByReference.values)
        val pendingPayments = resolvedPaymentRequestsByExpenseReference.flatMap { (expenseReference, resolvedPaymentRequests) ->
            val expenseRecord = expenseRecordsByReference.getValue(expenseReference)
            planPayments(expenseRecord, expenseAggregatesById.getValue(expenseRecord.id), resolvedPaymentRequests)
        }
        saveAndPublish(expenseStore, pendingPayments, expenseAggregatesById)
        return expenseAggregatesById.map { (expenseId, expenseAggregate) -> expenseResponseBuilder.buildSummary(expenseAggregate, expenseId) }
    }

    private fun requireWithinRequestLimits(expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>) {
        if (expensePaymentCreateRequests.isEmpty()) throw RtsGenericException("At least one payment is required")
        if (expensePaymentCreateRequests.size > ExpenseRowResolver.MAXIMUM_ROWS_PER_REQUEST) {
            throw RtsGenericException("A request can carry at most ${ExpenseRowResolver.MAXIMUM_ROWS_PER_REQUEST} payments")
        }
    }

    private fun resolveByExpenseReference(
        expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>
    ): Map<String, List<ResolvedPaymentRequest>> {
        val defaultPaymentDate = DateTimes.Local.Now.organization()
        return expensePaymentCreateRequests
            .map {
                val resolvedSettlement = expenseRowResolver.resolveSettlement(it.settlement, defaultPaymentDate)
                ResolvedPaymentRequest(it, resolvedSettlement)
            }
            .groupBy { it.expensePaymentCreateRequest.expenseReference }
    }

    // Locks are taken in id order so two bulk requests over the same expenses cannot deadlock.
    private fun lockAndLoad(
        expenseStore: ExpenseStore,
        expenseRecords: Collection<ExpenseRecord>
    ): MutableMap<UUID, ExpenseAggregate> {
        val expenseIdsInLockOrder = expenseRecords.map { it.id }.sorted()
        expenseIdsInLockOrder.forEach { expenseStore.lockExpense(it) }
        val loadedAggregate = expenseStore.loadForExpenses(expenseIdsInLockOrder)
        return expenseRecords.associate { it.id to loadedAggregate.forExpense(it.id) }.toMutableMap()
    }

    private fun planPayments(
        expenseRecord: ExpenseRecord,
        expenseAggregate: ExpenseAggregate,
        resolvedPaymentRequests: List<ResolvedPaymentRequest>
    ): List<PendingPayment> {
        expenseLookup.requireNotVoided(expenseAggregate, expenseRecord)
        val pendingPayments = resolvedPaymentRequests.map { (paymentRequest, resolvedSettlement) ->
            val paymentAmount = Decimals.roundToScale4(paymentRequest.amount)
            if (paymentAmount <= BigDecimal.ZERO) throw RtsGenericException("Payment amount must be greater than zero")
            PendingPayment(expenseRecord, ExpensePaymentDraft(expenseRecord.id, paymentAmount, resolvedSettlement))
        }
        val remainingBalance = expenseRecord.amount - activePaidAmount(expenseAggregate)
        val totalRequested = pendingPayments.sumOf { it.expensePaymentDraft.amount }
        if (totalRequested > remainingBalance) {
            val formattedRemainingBalance = DisplayFormatters.formatCurrency(remainingBalance)
            val formattedTotalRequested = DisplayFormatters.formatCurrency(totalRequested)
            val formattedDifference = DisplayFormatters.formatCurrency(totalRequested - remainingBalance)
            throw RtsGenericException(
                "Payments totalling $formattedTotalRequested exceed the remaining balance of $formattedRemainingBalance " +
                    "on ${expenseRecord.referenceNumber} by $formattedDifference"
            )
        }
        return pendingPayments
    }

    private fun saveAndPublish(
        expenseStore: ExpenseStore,
        pendingPayments: List<PendingPayment>,
        expenseAggregatesById: MutableMap<UUID, ExpenseAggregate>
    ) {
        // saveAll returns entities in input order, which is what pairs each saved record with its pending payment.
        val savedPaymentRecords = expenseStore.savePayments(pendingPayments.map { it.expensePaymentDraft })
        pendingPayments.zip(savedPaymentRecords).forEach { (pendingPayment, expensePaymentRecord) ->
            publishPaymentRecorded(expenseStore, expensePaymentRecord, pendingPayment.expenseRecord)
            val expenseAggregate = expenseAggregatesById.getValue(pendingPayment.expenseRecord.id)
            expenseAggregatesById[pendingPayment.expenseRecord.id] =
                expenseAggregate.copy(payments = expenseAggregate.payments + expensePaymentRecord)
        }
    }

    fun settle(
        expenseStore: ExpenseStore,
        expenseRecord: ExpenseRecord,
        amount: BigDecimal,
        resolvedSettlement: ResolvedSettlement
    ): ExpensePaymentRecord {
        val expensePaymentRecord = expenseStore.savePayments(listOf(
            ExpensePaymentDraft(expenseRecord.id, amount, resolvedSettlement))
        ).single()
        publishPaymentRecorded(expenseStore, expensePaymentRecord, expenseRecord)
        return expensePaymentRecord
    }

    fun voidPayment(expenseStore: ExpenseStore, expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse {
        val voidReason = StringUtils.getValueOrException(expensePaymentVoidRequest.reason, "A void reason is required")
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
        val expensePaymentRecord = expenseStore.findPaymentByReference(expensePaymentVoidRequest.paymentReference)
            ?: throw RtsGenericException("Expense payment ${expensePaymentVoidRequest.paymentReference} not found")
        expenseStore.lockExpense(expensePaymentRecord.expenseId)
        val expenseAggregate = expenseStore.loadForExpenses(listOf(expensePaymentRecord.expenseId))
        if (expenseAggregate.paymentVoids.any { it.paymentId == expensePaymentRecord.id }) {
            throw RtsGenericException("Payment ${expensePaymentRecord.referenceNumber} has already been voided")
        }
        val expenseRecord = expenseAggregate.expenses.single()
        val expensePaymentVoidRecord = expenseStore.savePaymentVoid(expensePaymentRecord.id, voidReason)
        publishPaymentVoided(expenseStore, expensePaymentVoidRecord, expensePaymentRecord, expenseRecord)
        return expenseResponseBuilder.buildSummary(
            expenseAggregate.copy(paymentVoids = expenseAggregate.paymentVoids + expensePaymentVoidRecord), expenseRecord.id
        )
    }

    fun reissuePaymentRecorded(expenseStore: ExpenseStore, paymentId: UUID) {
        val expensePaymentRecord = requirePaymentById(expenseStore, paymentId)
        val expenseRecord = expenseLookup.requireExpenseById(expenseStore, expensePaymentRecord.expenseId)
        publishPaymentRecorded(expenseStore, expensePaymentRecord, expenseRecord)
    }

    fun reissuePaymentVoided(expenseStore: ExpenseStore, paymentVoidId: UUID) {
        val expensePaymentVoidRecord = expenseStore.findPaymentVoidById(paymentVoidId)
            ?: throw RtsGenericException("Expense payment void $paymentVoidId not found")
        val expensePaymentRecord = requirePaymentById(expenseStore, expensePaymentVoidRecord.paymentId)
        val expenseRecord = expenseLookup.requireExpenseById(expenseStore, expensePaymentRecord.expenseId)
        publishPaymentVoided(expenseStore, expensePaymentVoidRecord, expensePaymentRecord, expenseRecord)
    }

    private fun publishPaymentRecorded(
        expenseStore: ExpenseStore,
        expensePaymentRecord: ExpensePaymentRecord,
        expenseRecord: ExpenseRecord
    ) {
        eventPublisher.publishEvent(
            ExpensePaymentRecordedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = expenseStore.sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                paymentId = expensePaymentRecord.id,
                paymentReferenceNumber = expensePaymentRecord.referenceNumber,
                expenseAccountCode = expenseRecord.expenseAccountCode,
                payeeContactId = expenseRecord.payeeContactId,
                paymentMethodAccountCode = expensePaymentRecord.paymentMethodAccountCode,
                amount = expensePaymentRecord.amount,
                paymentDate = expensePaymentRecord.paymentDate
            )
        )
    }

    private fun publishPaymentVoided(
        expenseStore: ExpenseStore,
        expensePaymentVoidRecord: ExpensePaymentVoidRecord,
        expensePaymentRecord: ExpensePaymentRecord,
        expenseRecord: ExpenseRecord
    ) {
        eventPublisher.publishEvent(
            ExpensePaymentVoidedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = expenseStore.sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                voidId = expensePaymentVoidRecord.id,
                paymentId = expensePaymentRecord.id,
                paymentReferenceNumber = expensePaymentRecord.referenceNumber,
                expenseAccountCode = expenseRecord.expenseAccountCode,
                payeeContactId = expenseRecord.payeeContactId,
                paymentMethodAccountCode = expensePaymentRecord.paymentMethodAccountCode,
                amount = expensePaymentRecord.amount,
                voidedOn = DateTimes.Local.atOrganizationZone(expensePaymentVoidRecord.voidedOn)
            )
        )
    }

    private fun requirePaymentById(expenseStore: ExpenseStore, paymentId: UUID): ExpensePaymentRecord =
        expenseStore.findPaymentById(paymentId) ?: throw RtsGenericException("Expense payment $paymentId not found")

    private fun activePaidAmount(expenseAggregate: ExpenseAggregate): BigDecimal {
        val voidedPaymentIds = expenseAggregate.paymentVoids.map { it.paymentId }.toSet()
        return expenseAggregate.payments.filter { it.id !in voidedPaymentIds }.sumOf { it.amount }
    }
}

private data class PendingPayment(
    val expenseRecord: ExpenseRecord,
    val expensePaymentDraft: ExpensePaymentDraft
)

private data class ResolvedPaymentRequest(
    val expensePaymentCreateRequest: ExpensePaymentCreateRequest,
    val resolvedSettlement: ResolvedSettlement
)
