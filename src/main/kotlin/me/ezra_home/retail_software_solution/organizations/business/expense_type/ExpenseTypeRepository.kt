package me.ezra_home.retail_software_solution.organizations.business.expense_type

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ExpenseTypeRepository : JpaRepository<ExpenseTypeEntity, UUID> {

    fun findByCode(code: String): ExpenseTypeEntity?

    fun findAllByExpenseAccountCode(expenseAccountCode: String): List<ExpenseTypeEntity>
}
