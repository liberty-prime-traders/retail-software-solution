package me.ezra_home.retail_software_solution.cross_tier.expense

import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import java.util.UUID

interface ExpenseTier {

    val expenseStore: ExpenseStore

    fun sourceContext(): EventSourceContext

    fun lockExpense(expenseId: UUID)

    fun lockExpenses(expenseIds: Collection<UUID>)

    fun lockSourceDocument(sourceDocumentId: UUID)
}
