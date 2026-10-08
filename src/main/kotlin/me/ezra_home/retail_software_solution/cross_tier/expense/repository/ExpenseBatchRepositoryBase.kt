package me.ezra_home.retail_software_solution.cross_tier.expense.repository

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBatchBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ExpenseBatchRepositoryBase<BATCH : ExpenseBatchBase> : JpaRepository<BATCH, UUID> {

    fun findBySourceTypeAndSourceReference(sourceType: ExpenseSourceType, sourceReference: String): BATCH?
}
