package me.ezra_home.retail_software_solution.cross_tier.expense.repository

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface ExpenseRepositoryBase<EXPENSE : ExpenseBase> : JpaRepository<EXPENSE, UUID> {

    fun findByReferenceNumber(referenceNumber: String): EXPENSE?

    fun findByReferenceNumberIn(referenceNumbers: Collection<String>): List<EXPENSE>

    fun findByBatchId(batchId: UUID): List<EXPENSE>
}
