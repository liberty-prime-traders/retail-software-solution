package me.ezra_home.retail_software_solution.organizations.business.adjustment_reason

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AdjustmentReasonRepository : JpaRepository<AdjustmentReasonEntity, UUID>
