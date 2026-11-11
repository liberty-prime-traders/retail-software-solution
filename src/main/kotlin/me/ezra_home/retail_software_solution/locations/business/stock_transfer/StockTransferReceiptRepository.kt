package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockTransferReceiptRepository : JpaRepository<StockTransferReceiptEntity, UUID> {

    fun findByStockTransferOrderRef(stockTransferOrderRef: String): StockTransferReceiptEntity?

    fun findByReferenceNumber(referenceNumber: String): StockTransferReceiptEntity?

    // A scalar read, so the receipt entity itself is first loaded after the order lock and cannot be a stale copy
    @Query(
        "select receipt.stockTransferOrderRef from StockTransferReceiptEntity receipt " +
            "where receipt.referenceNumber = :referenceNumber"
    )
    fun findStockTransferOrderRefByReferenceNumber(@Param("referenceNumber") referenceNumber: String): String?
}
