package me.ezra_home.retail_software_solution.organizations.business.loan

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LoanRepaymentRepository : JpaRepository<LoanRepaymentEntity, UUID> {

    fun findTop10ByAssetAccountCodeOrderByCreatedOnDesc(assetAccountCode: String): List<LoanRepaymentEntity>

    fun findByLoanIdIn(loanIds: Collection<UUID>): List<LoanRepaymentEntity>
}
