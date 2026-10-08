package me.ezra_home.retail_software_solution.locations.business.expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchOperations
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchSummaryResponseDto
import me.ezra_home.retail_software_solution.locations.business.expense.search.ExpenseSearchFetcher
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class ExpenseSearchService(
    private val expenseSearchOperations: ExpenseSearchOperations,
    private val expenseSearchFetcher: ExpenseSearchFetcher,
    private val locationExpenseStore: LocationExpenseStore
) {

    fun search(pageRequest: PageRequest<ExpenseSearchParameters, String>): PageResponse<ExpenseSummaryResponse, String> =
        expenseSearchOperations.search(expenseSearchFetcher, locationExpenseStore, pageRequest)

    fun summarize(expenseSearchParameters: ExpenseSearchParameters): ExpenseSearchSummaryResponseDto =
        expenseSearchOperations.summarize(expenseSearchFetcher, expenseSearchParameters)
}
