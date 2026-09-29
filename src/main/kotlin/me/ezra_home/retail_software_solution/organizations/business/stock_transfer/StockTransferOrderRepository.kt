package me.ezra_home.retail_software_solution.organizations.business.stock_transfer

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockTransferOrderRepository : JpaRepository<StockTransferOrderEntity, UUID> {

    fun findByReferenceNumber(referenceNumber: String): StockTransferOrderEntity?

    @Query("" +
            "SELECT o FROM StockTransferOrderEntity o" +
            " WHERE o.sourceLocationId = :locationId " +
            " OR o.destinationLocationId = :locationId " +
            " ORDER BY o.createdOn DESC")
    fun findTopNByLocation(locationId: UUID, pageable: Pageable): List<StockTransferOrderEntity>

    fun findAllByOrderByCreatedOnDesc(pageable: Pageable): List<StockTransferOrderEntity>
}
