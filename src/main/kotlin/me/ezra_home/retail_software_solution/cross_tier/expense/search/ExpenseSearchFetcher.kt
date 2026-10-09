package me.ezra_home.retail_software_solution.cross_tier.expense.search

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.SqlSearchExecutor

class ExpenseSearchFetcher(
    private val sqlSearchExecutor: SqlSearchExecutor<Tuple, Tuple>,
    private val expenseSearchTables: ExpenseSearchTables
) {

    fun search(expenseSearchParameters: ExpenseSearchParameters, cursor: KeysetSearchCursor?, requestedSize: Int): List<ExpenseSearchRawRow> {
        val predicate = ExpenseSearchQueryBuilder.buildPredicate(expenseSearchParameters, expenseSearchTables)
        val sqlQuery = ExpenseSearchQueryBuilder.buildListQuery(predicate, cursor, expenseSearchTables)
        return sqlSearchExecutor.execute(sqlQuery, requestedSize + 1, setTimeout = true)
            .map { ExpenseSearchRowMapper.fromTuple(it) }
    }

    fun summarize(expenseSearchParameters: ExpenseSearchParameters): List<ExpenseSummaryRawRow> {
        val predicate = ExpenseSearchQueryBuilder.buildPredicate(expenseSearchParameters, expenseSearchTables)
        val sqlQuery = ExpenseSearchQueryBuilder.buildSummaryQuery(predicate, expenseSearchTables)
        return sqlSearchExecutor.executeUnpaged(sqlQuery, setTimeout = true)
            .map { ExpenseSearchRowMapper.summaryFromTuple(it) }
    }
}
