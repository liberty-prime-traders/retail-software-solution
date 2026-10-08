package me.ezra_home.retail_software_solution.organizations.business.org_expense.search

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchPort
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchQueryBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchRawRow
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchRowMapper
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSearchTables
import me.ezra_home.retail_software_solution.cross_tier.expense.search.ExpenseSummaryRawRow
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema(readOnly = true)
class OrgExpenseSearchFetcher(
    private val orgExpenseSearchExecutor: OrgExpenseSearchExecutor
) : ExpenseSearchPort {

    override fun search(
        expenseSearchParameters: ExpenseSearchParameters,
        cursor: KeysetSearchCursor?,
        requestedSize: Int
    ): List<ExpenseSearchRawRow> {
        val predicate = ExpenseSearchQueryBuilder.buildPredicate(expenseSearchParameters, ExpenseSearchTables.ORGANIZATION)
        val sqlQuery = ExpenseSearchQueryBuilder.buildListQuery(predicate, cursor, ExpenseSearchTables.ORGANIZATION)
        return orgExpenseSearchExecutor.execute(sqlQuery, requestedSize + 1, setTimeout = true)
            .map { ExpenseSearchRowMapper.fromTuple(it) }
    }

    override fun summarize(expenseSearchParameters: ExpenseSearchParameters): List<ExpenseSummaryRawRow> {
        val predicate = ExpenseSearchQueryBuilder.buildPredicate(expenseSearchParameters, ExpenseSearchTables.ORGANIZATION)
        val sqlQuery = ExpenseSearchQueryBuilder.buildSummaryQuery(predicate, ExpenseSearchTables.ORGANIZATION)
        return orgExpenseSearchExecutor.executeUnpaged(sqlQuery, setTimeout = true)
            .map { ExpenseSearchRowMapper.summaryFromTuple(it) }
    }
}
