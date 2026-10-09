package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpensePaymentCreateRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpensePaymentVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ResolvedSettlement
import me.ezra_home.retail_software_solution.cross_tier.expense.model.SettledExpense
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseTier
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class ExpensePaymentOperations(
    private val expenseTier: ExpenseTier,
    private val expenseEvents: ExpenseEvents,
    private val expenseRowResolver: ExpenseRowResolver,
    private val expenseResponseBuilder: ExpenseResponseBuilder,
    private val expenseLookup: ExpenseLookup
) {

    private val expenseStore = expenseTier.expenseStore

    fun recordPayments(
        expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>
    ): List<ExpenseSummaryResponse> {
        requireWithinRequestLimits(expensePaymentCreateRequests)
        val resolvedPaymentRequestsByExpenseReference = resolveByExpenseReference(expensePaymentCreateRequests)
        val expenseDtosByReference = expenseLookup.requireExpenses(resolvedPaymentRequestsByExpenseReference.keys.toList())
        val expenseAggregatesById = lockAndLoad(expenseDtosByReference.values)
        val expensePaymentDrafts = resolvedPaymentRequestsByExpenseReference.flatMap { (expenseReference, resolvedPaymentRequests) ->
            val expenseDto = expenseDtosByReference.getValue(expenseReference)
            planPayments(expenseDto, expenseAggregatesById.getValue(expenseDto.id), resolvedPaymentRequests)
        }
        saveAndPublish(expensePaymentDrafts, expenseDtosByReference.values)
        expenseStore.refreshPaymentStates(expenseDtosByReference.values)
        val summariesByReference = expenseResponseBuilder
            .buildSummaries(expenseStore.loadForExpenses(expenseDtosByReference.values.map { it.id }))
            .associateBy { it.reference }
        return resolvedPaymentRequestsByExpenseReference.keys.map { summariesByReference.getValue(it) }
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
        val openDates = HashSet<LocalDate>()
        return expensePaymentCreateRequests
            .map {
                val resolvedSettlement = expenseRowResolver.resolveSettlement(it.settlement, defaultPaymentDate, openDates)
                ResolvedPaymentRequest(it, resolvedSettlement)
            }
            .groupBy { it.expensePaymentCreateRequest.expenseReference }
    }

    private fun lockAndLoad(expenseDtos: Collection<ExpenseDto>): Map<UUID, ExpenseAggregate> {
        val expenseIds = expenseDtos.map { it.id }
        expenseTier.lockExpenses(expenseIds)
        val loadedAggregate = expenseStore.loadForExpenses(expenseIds)
        return expenseDtos.associate { it.id to loadedAggregate.forExpense(it.id) }
    }

    private fun planPayments(
        expenseDto: ExpenseDto,
        expenseAggregate: ExpenseAggregate,
        resolvedPaymentRequests: List<ResolvedPaymentRequest>
    ): List<ExpensePaymentDraft> {
        expenseLookup.requireNotVoided(expenseAggregate, expenseDto)
        val expensePaymentDrafts = resolvedPaymentRequests.map { (paymentRequest, resolvedSettlement) ->
            val paymentAmount = Decimals.roundToScale4(paymentRequest.amount)
            if (paymentAmount <= BigDecimal.ZERO) throw RtsGenericException("Payment amount must be greater than zero")
            ExpensePaymentDraft(expenseDto.id, paymentAmount, resolvedSettlement)
        }
        val remainingBalance = expenseDto.amount - expenseAggregate.activePaidAmount(expenseDto.id)
        val totalRequested = expensePaymentDrafts.sumOf { it.amount }
        if (totalRequested > remainingBalance) {
            val formattedRemainingBalance = DisplayFormatters.formatCurrency(remainingBalance)
            val formattedTotalRequested = DisplayFormatters.formatCurrency(totalRequested)
            val formattedDifference = DisplayFormatters.formatCurrency(totalRequested - remainingBalance)
            throw RtsGenericException(
                "Payments totalling $formattedTotalRequested exceed the remaining balance of $formattedRemainingBalance " +
                    "on ${expenseDto.referenceNumber} by $formattedDifference"
            )
        }
        return expensePaymentDrafts
    }

    private fun saveAndPublish(expensePaymentDrafts: List<ExpensePaymentDraft>, expenseDtos: Collection<ExpenseDto>) {
        publishRecorded(expenseStore.savePayments(expensePaymentDrafts), expenseDtos)
    }

    fun settleNewExpenses(settledExpenses: List<SettledExpense>) {
        if (settledExpenses.isEmpty()) return
        val expensePaymentDrafts = settledExpenses.map {
            ExpensePaymentDraft(it.expenseDto.id, it.expenseDto.amount, it.resolvedSettlement)
        }
        val expenseDtos = settledExpenses.map { it.expenseDto }
        publishRecorded(expenseStore.savePayments(expensePaymentDrafts), expenseDtos)
        expenseStore.refreshPaymentStates(expenseDtos)
    }

    private fun publishRecorded(savedPaymentDtos: List<ExpensePaymentDto>, expenseDtos: Collection<ExpenseDto>) {
        val expenseDtosById = expenseDtos.associateBy { it.id }
        savedPaymentDtos.forEach { expensePaymentDto ->
            expenseEvents.publishPaymentRecorded(expensePaymentDto, expenseDtosById.getValue(expensePaymentDto.expenseId))
        }
    }

    fun voidPayment(expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse {
        val voidReason = StringUtils.getValueOrException(expensePaymentVoidRequest.reason, "A void reason is required")
        expenseRowResolver.requireOpenPeriodToday()
        val expensePaymentDto = expenseStore.findPaymentByReference(expensePaymentVoidRequest.paymentReference)
            ?: throw RtsGenericException("Expense payment ${expensePaymentVoidRequest.paymentReference} not found")
        expenseTier.lockExpense(expensePaymentDto.expenseId)
        val expenseAggregate = expenseStore.loadForExpenses(listOf(expensePaymentDto.expenseId))
        if (expenseAggregate.paymentVoids.any { it.paymentId == expensePaymentDto.id }) {
            throw RtsGenericException("Payment ${expensePaymentDto.referenceNumber} has already been voided")
        }
        val expenseDto = expenseAggregate.expenses.single()
        val expensePaymentVoidDto = expenseStore.savePaymentVoid(expensePaymentDto.id, voidReason)
        expenseStore.refreshPaymentStates(listOf(expenseDto))
        expenseEvents.publishPaymentVoided(expensePaymentVoidDto, expensePaymentDto, expenseDto)
        return expenseResponseBuilder.buildSummary(expenseStore.loadForExpenses(listOf(expenseDto.id)), expenseDto.id)
    }
}

private data class ResolvedPaymentRequest(
    val expensePaymentCreateRequest: ExpensePaymentCreateRequest,
    val resolvedSettlement: ResolvedSettlement
)
