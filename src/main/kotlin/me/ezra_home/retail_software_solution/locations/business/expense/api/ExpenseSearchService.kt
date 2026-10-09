package me.ezra_home.retail_software_solution.locations.business.expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchFetcher
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchOperationsFactory
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchSummaryResponseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchTables
import me.ezra_home.retail_software_solution.locations.business.expense.search.ExpenseSearchExecutor
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class ExpenseSearchService(
    expenseSearchOperationsFactory: ExpenseSearchOperationsFactory,
    locationExpenseStore: LocationExpenseStore,
    expenseSearchExecutor: ExpenseSearchExecutor
) {

    private val expenseSearchOperations = expenseSearchOperationsFactory.operationsFor(
        locationExpenseStore, ExpenseSearchFetcher(expenseSearchExecutor, ExpenseSearchTables.LOCATION)
    )

    fun search(pageRequest: PageRequest<ExpenseSearchParameters, String>): PageResponse<ExpenseSummaryResponse, String> =
        expenseSearchOperations.search(pageRequest)

    fun summarize(expenseSearchParameters: ExpenseSearchParameters): ExpenseSearchSummaryResponseDto =
        expenseSearchOperations.summarize(expenseSearchParameters)
}
