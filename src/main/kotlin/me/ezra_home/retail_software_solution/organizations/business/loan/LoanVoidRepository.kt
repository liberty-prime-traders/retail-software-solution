package me.ezra_home.retail_software_solution.organizations.business.loan

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LoanVoidRepository : JpaRepository<LoanVoidEntity, UUID> {
    fun existsByLoanId(loanId: UUID): Boolean

    fun findByLoanIdIn(loanIds: Collection<UUID>): List<LoanVoidEntity>
}
