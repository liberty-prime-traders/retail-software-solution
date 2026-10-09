package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeService
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import java.util.UUID

class ExpenseSearchOperations(
    private val expenseSearchFetcher: ExpenseSearchFetcher,
    private val expenseStore: ExpenseStore,
    private val expenseTypeService: ExpenseTypeService,
    private val expenseResponseBuilder: ExpenseResponseBuilder
) {

    fun search(pageRequest: PageRequest<ExpenseSearchParameters, String>): PageResponse<ExpenseSummaryResponse, String> {
        val expenseSearchParameters = pageRequest.parameters.sanitized()
        ExpenseSearchValidator.guardValidParameters(expenseSearchParameters)
        ExpenseSearchValidator.guardValidPageSize(pageRequest.requestedSize)
        val cursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

        val rawRows = expenseSearchFetcher.search(expenseSearchParameters, cursor, pageRequest.requestedSize)
        val hasMore = rawRows.size > pageRequest.requestedSize
        val pageRows = if (hasMore) rawRows.take(pageRequest.requestedSize) else rawRows

        val summariesByReference = summariesByReference(pageRows.map { it.id })
        val contents = pageRows.map { summariesByReference.getValue(it.referenceNumber) }

        val currentCursor = pageRows.lastOrNull()
            ?.let { KeysetSearchCursor(it.createdOn, it.id).encode() }
            ?: pageRequest.previousCursor

        return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
    }

    fun summarize(expenseSearchParameters: ExpenseSearchParameters): ExpenseSearchSummaryResponseDto {
        val sanitizedExpenseSearchParameters = expenseSearchParameters.sanitized()
        ExpenseSearchValidator.guardValidParameters(sanitizedExpenseSearchParameters)
        val rawRows = expenseSearchFetcher.summarize(sanitizedExpenseSearchParameters)

        val byStatus = toBucketSummaries(rawRows)
        val expenseTypeNamesById = expenseTypeNamesById()
        val byExpenseType = rawRows.groupBy { it.expenseTypeId }
            .map { (expenseTypeId, expenseTypeRows) ->
                val (voidedRows, liveRows) = expenseTypeRows.partition { it.voided }
                ExpenseTypeSummaryDto(
                    expenseTypeId = expenseTypeId,
                    expenseTypeName = expenseTypeNamesById.getValue(expenseTypeId),
                    expenseCount = liveRows.sumOf { it.expenseCount },
                    amountTotal = liveRows.sumOf { it.amountTotal },
                    paidTotal = liveRows.sumOf { it.paidTotal },
                    outstandingTotal = liveRows.sumOf { it.outstandingTotal },
                    voidedCount = voidedRows.sumOf { it.expenseCount },
                    voidedAmountTotal = voidedRows.sumOf { it.amountTotal }
                )
            }
            .sortedBy { it.expenseTypeName }

        return ExpenseSearchSummaryResponseDto(
            byStatus = byStatus,
            byExpenseType = byExpenseType,
            expenseCount = byExpenseType.sumOf { it.expenseCount },
            amountTotal = byExpenseType.sumOf { it.amountTotal },
            paidTotal = byExpenseType.sumOf { it.paidTotal },
            outstandingTotal = byExpenseType.sumOf { it.outstandingTotal },
            voidedCount = byExpenseType.sumOf { it.voidedCount },
            voidedAmountTotal = byExpenseType.sumOf { it.voidedAmountTotal }
        )
    }

    private fun summariesByReference(expenseIds: List<UUID>): Map<String, ExpenseSummaryResponse> {
        if (expenseIds.isEmpty()) return emptyMap()
        return expenseResponseBuilder.buildSummaries(expenseStore.loadForExpenses(expenseIds)).associateBy { it.reference }
    }

    private fun toBucketSummaries(rawRows: List<ExpenseSummaryRawRow>): List<ExpenseBucketSummaryDto> {
        val rawRowsByBucket = rawRows.groupBy { ExpenseSearchMapper.toBucket(it.voided, it.paymentStatus) }
        return ExpenseSummaryBucket.entries.map { bucket ->
            val bucketRows = rawRowsByBucket[bucket].orEmpty()
            ExpenseBucketSummaryDto(
                bucket = bucket,
                expenseCount = bucketRows.sumOf { it.expenseCount },
                amountTotal = bucketRows.sumOf { it.amountTotal },
                paidTotal = bucketRows.sumOf { it.paidTotal },
                outstandingTotal = bucketRows.sumOf { it.outstandingTotal }
            )
        }
    }

    private fun expenseTypeNamesById(): Map<UUID, String> =
        expenseTypeService.getAll(null).associate { it.id to it.name }
}
