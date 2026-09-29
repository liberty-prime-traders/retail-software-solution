package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockTransferReceiptRepository : JpaRepository<StockTransferReceiptEntity, UUID> {

    fun findByStockTransferOrderRef(stockTransferOrderRef: String): StockTransferReceiptEntity?

    fun findByReferenceNumber(referenceNumber: String): StockTransferReceiptEntity?
}
