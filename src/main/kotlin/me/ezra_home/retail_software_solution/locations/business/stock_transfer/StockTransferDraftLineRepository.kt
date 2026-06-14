package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockTransferDraftLineRepository : JpaRepository<StockTransferDraftLineEntity, UUID> {

    fun findByStockTransferDispatchId(stockTransferDispatchId: UUID): List<StockTransferDraftLineEntity>

    fun findByReferenceNumber(referenceNumber: String): StockTransferDraftLineEntity?

    fun existsByStockTransferDispatchIdAndLocationProductId(stockTransferDispatchId: UUID, locationProductId: UUID): Boolean

    fun deleteByStockTransferDispatchId(stockTransferDispatchId: UUID)
}
