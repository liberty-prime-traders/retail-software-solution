package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.util.queries.SearchGuards

object ExpenseSearchValidator {

    private const val MAX_EXPENSE_REFERENCE_NUMBERS = 20
    private const val MAX_SOURCE_REFERENCES = 20
    private const val MAX_PAYEE_CONTACT_IDS = 100
    private const val MAX_EXPENSE_TYPE_IDS = 100
    private const val MAX_PAYMENT_METHOD_IDS = 100
    private const val MIN_PAGE_SIZE = 1
    private const val MAX_PAGE_SIZE = 500

    fun guardValidParameters(expenseSearchParameters: ExpenseSearchParameters) {
        SearchGuards.guardBoundedRange(
            expenseSearchParameters.createdFrom,
            expenseSearchParameters.createdBefore,
            "createdFrom",
            "createdBefore"
        )
        SearchGuards.guardMaxSize(expenseSearchParameters.expenseReferenceNumbers.size, MAX_EXPENSE_REFERENCE_NUMBERS, "expense reference numbers")
        SearchGuards.guardMaxSize(expenseSearchParameters.sourceReferences.size, MAX_SOURCE_REFERENCES, "source references")
        SearchGuards.guardMaxSize(expenseSearchParameters.payeeContactIds.size, MAX_PAYEE_CONTACT_IDS, "payee contact ids")
        SearchGuards.guardMaxSize(expenseSearchParameters.expenseTypeIds.size, MAX_EXPENSE_TYPE_IDS, "expense type ids")
        SearchGuards.guardMaxSize(expenseSearchParameters.paymentMethodIds.size, MAX_PAYMENT_METHOD_IDS, "payment method ids")

        SearchGuards.guardRangeOrder(
            expenseSearchParameters.createdFrom,
            expenseSearchParameters.createdBefore,
            "createdFrom must not be after createdBefore"
        ) { from, before -> from.isAfter(before) }

        SearchGuards.guardRangeOrder(
            expenseSearchParameters.expenseDateFrom,
            expenseSearchParameters.expenseDateBefore,
            "expenseDateFrom must not be after expenseDateBefore"
        ) { from, before -> from.isAfter(before) }

        SearchGuards.guardRangeOrder(
            expenseSearchParameters.minAmount,
            expenseSearchParameters.maxAmount,
            "minAmount must not be greater than maxAmount"
        ) { min, max -> min > max }
        SearchGuards.guardNonNegative(expenseSearchParameters.minAmount, "minAmount")
        SearchGuards.guardNonNegative(expenseSearchParameters.maxAmount, "maxAmount")
    }

    fun guardValidPageSize(requestedSize: Int) {
        SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
    }
}
