package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockTransferDispatchRepository : JpaRepository<StockTransferDispatchEntity, UUID> {

    fun findByStockTransferOrderRef(stockTransferOrderRef: String): StockTransferDispatchEntity?

    fun findByReferenceNumber(referenceNumber: String): StockTransferDispatchEntity?
}
