package me.ezra_home.retail_software_solution.organizations.business.org_expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchFetcher
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchOperationsFactory
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchSummaryResponseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchTables
import me.ezra_home.retail_software_solution.organizations.business.org_expense.search.OrgExpenseSearchExecutor
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class OrgExpenseSearchService(
    expenseSearchOperationsFactory: ExpenseSearchOperationsFactory,
    orgExpenseStore: OrgExpenseStore,
    orgExpenseSearchExecutor: OrgExpenseSearchExecutor
) {

    private val expenseSearchOperations = expenseSearchOperationsFactory.operationsFor(
        orgExpenseStore, ExpenseSearchFetcher(orgExpenseSearchExecutor, ExpenseSearchTables.ORGANIZATION)
    )

    fun search(pageRequest: PageRequest<ExpenseSearchParameters, String>): PageResponse<ExpenseSummaryResponse, String> =
        expenseSearchOperations.search(pageRequest)

    fun summarize(expenseSearchParameters: ExpenseSearchParameters): ExpenseSearchSummaryResponseDto =
        expenseSearchOperations.summarize(expenseSearchParameters)
}
