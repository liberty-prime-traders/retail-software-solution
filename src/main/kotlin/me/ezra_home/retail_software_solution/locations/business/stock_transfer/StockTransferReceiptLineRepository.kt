package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockTransferReceiptLineRepository : JpaRepository<StockTransferReceiptLineEntity, UUID> {

    fun findByStockTransferReceiptId(stockTransferReceiptId: UUID): List<StockTransferReceiptLineEntity>

    fun findByStockTransferDispatchLineRef(stockTransferDispatchLineRef: String): StockTransferReceiptLineEntity?

    fun existsByStockTransferDispatchLineRef(stockTransferDispatchLineRef: String): Boolean
}
