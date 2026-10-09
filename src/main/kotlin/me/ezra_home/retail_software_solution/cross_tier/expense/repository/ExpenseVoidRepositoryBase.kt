package me.ezra_home.retail_software_solution.cross_tier.expense.repository

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseVoidBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ExpenseVoidRepositoryBase<EXPENSE_VOID : ExpenseVoidBase> : JpaRepository<EXPENSE_VOID, UUID> {
    fun findByExpenseIdIn(expenseIds: Collection<UUID>): List<EXPENSE_VOID>
}
