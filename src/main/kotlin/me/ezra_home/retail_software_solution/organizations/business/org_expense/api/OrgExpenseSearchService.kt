package me.ezra_home.retail_software_solution.organizations.business.org_expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchOperations
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchSummaryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.org_expense.search.OrgExpenseSearchFetcher
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class OrgExpenseSearchService(
    private val expenseSearchOperations: ExpenseSearchOperations,
    private val orgExpenseSearchFetcher: OrgExpenseSearchFetcher,
    private val orgExpenseStore: OrgExpenseStore
) {

    fun search(pageRequest: PageRequest<ExpenseSearchParameters, String>): PageResponse<ExpenseSummaryResponse, String> =
        expenseSearchOperations.search(orgExpenseSearchFetcher, orgExpenseStore, pageRequest)

    fun summarize(expenseSearchParameters: ExpenseSearchParameters): ExpenseSearchSummaryResponseDto =
        expenseSearchOperations.summarize(orgExpenseSearchFetcher, expenseSearchParameters)
}
