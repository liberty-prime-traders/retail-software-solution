package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FundMovementRepository : JpaRepository<FundMovementEntity, UUID> {
    fun findTop10ByDebitAccountCodeOrCreditAccountCodeOrderByCreatedOnDesc(
        debitAccountCode: String,
        creditAccountCode: String
    ): List<FundMovementEntity>
}
