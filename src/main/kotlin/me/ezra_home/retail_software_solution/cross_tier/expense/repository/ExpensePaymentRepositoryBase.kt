package me.ezra_home.retail_software_solution.cross_tier.expense.repository

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ExpensePaymentRepositoryBase<PAYMENT : ExpensePaymentBase> : JpaRepository<PAYMENT, UUID> {

    fun findByReferenceNumber(referenceNumber: String): PAYMENT?

    fun findByExpenseIdIn(expenseIds: Collection<UUID>): List<PAYMENT>
}
