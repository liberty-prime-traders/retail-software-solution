package me.ezra_home.retail_software_solution.cross_tier.expense.repository

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentVoidBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ExpensePaymentVoidRepositoryBase<PAYMENT_VOID : ExpensePaymentVoidBase> : JpaRepository<PAYMENT_VOID, UUID> {
    fun findByExpensePaymentIdIn(expensePaymentIds: Collection<UUID>): List<PAYMENT_VOID>
}
