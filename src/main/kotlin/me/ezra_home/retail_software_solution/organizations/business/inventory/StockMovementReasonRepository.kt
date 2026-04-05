package me.ezra_home.retail_software_solution.organizations.business.inventory

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.StockMovementReasonEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockMovementReasonRepository : JpaRepository<StockMovementReasonEntity, UUID>
