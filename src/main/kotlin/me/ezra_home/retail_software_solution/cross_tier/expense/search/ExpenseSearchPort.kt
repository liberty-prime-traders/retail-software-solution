package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor

interface ExpenseSearchPort {

    fun search(expenseSearchParameters: ExpenseSearchParameters, cursor: KeysetSearchCursor?, requestedSize: Int): List<ExpenseSearchRawRow>

    fun summarize(expenseSearchParameters: ExpenseSearchParameters): List<ExpenseSummaryRawRow>
}
