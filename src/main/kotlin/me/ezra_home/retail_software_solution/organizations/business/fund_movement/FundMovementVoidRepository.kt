package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FundMovementVoidRepository : JpaRepository<FundMovementVoidEntity, UUID> {
    fun existsByFundMovementId(fundMovementId: UUID): Boolean
}
