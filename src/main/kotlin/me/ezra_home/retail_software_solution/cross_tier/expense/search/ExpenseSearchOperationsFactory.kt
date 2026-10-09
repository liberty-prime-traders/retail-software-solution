package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeService
import org.springframework.stereotype.Component

@Component
class ExpenseSearchOperationsFactory(
    private val expenseTypeService: ExpenseTypeService,
    private val expenseResponseBuilder: ExpenseResponseBuilder
) {

    fun operationsFor(expenseStore: ExpenseStore, expenseSearchFetcher: ExpenseSearchFetcher) =
        ExpenseSearchOperations(expenseSearchFetcher, expenseStore, expenseTypeService, expenseResponseBuilder)
}
