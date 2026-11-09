package me.ezra_home.retail_software_solution.locations.business.stock_adjustment

import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentCostStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockAdjustmentCostRepository : JpaRepository<StockAdjustmentCostEntity, UUID> {

    fun findByStockAdjustmentIdInAndStatus(
        stockAdjustmentIds: Collection<UUID>,
        status: StockAdjustmentCostStatus
    ): List<StockAdjustmentCostEntity>

    fun countByStatus(status: StockAdjustmentCostStatus): Long
}
