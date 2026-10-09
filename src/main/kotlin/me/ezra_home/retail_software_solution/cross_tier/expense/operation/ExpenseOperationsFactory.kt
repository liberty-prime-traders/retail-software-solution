package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseTier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ExpenseOperationsFactory(
    private val expenseRowResolver: ExpenseRowResolver,
    private val expenseResponseBuilder: ExpenseResponseBuilder,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun operationsFor(expenseTier: ExpenseTier): TierExpenseOperations {
        val expenseEvents = ExpenseEvents(expenseTier, applicationEventPublisher)
        val expenseLookup = ExpenseLookup(expenseTier.expenseStore)
        val expensePaymentOperations = ExpensePaymentOperations(
            expenseTier, expenseEvents, expenseRowResolver, expenseResponseBuilder, expenseLookup
        )
        val expenseOperations = ExpenseOperations(
            expenseTier, expenseEvents, expenseRowResolver, expenseResponseBuilder, expenseLookup, expensePaymentOperations
        )
        val expenseReadOperations = ExpenseReadOperations(expenseTier, expenseResponseBuilder, expenseLookup)
        val expenseReissueOperations = ExpenseReissueOperations(expenseTier, expenseEvents, expenseLookup)
        return TierExpenseOperations(expenseOperations, expensePaymentOperations, expenseReadOperations, expenseReissueOperations)
    }
}

data class TierExpenseOperations(
    val expenseOperations: ExpenseOperations,
    val expensePaymentOperations: ExpensePaymentOperations,
    val expenseReadOperations: ExpenseReadOperations,
    val expenseReissueOperations: ExpenseReissueOperations
)
