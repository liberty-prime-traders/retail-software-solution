package me.ezra_home.retail_software_solution.locations.business.stock

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockReservationRepository : JpaRepository<StockReservationEntity, UUID> {
    fun deleteBySaleId(saleId: UUID)
    fun deleteBySaleLineIdIn(saleLineIds: List<UUID>)

    @Query(
        "SELECT new me.ezra_home.retail_software_solution.locations.business.stock.ProductReservationRow(" +
                "r.locationProductId, r.saleId, SUM(r.storageQuantityReserved)) " +
                "FROM StockReservationEntity r " +
                "WHERE r.locationProductId IN :locationProductIds " +
                "GROUP BY r.locationProductId, r.saleId"
    )
    fun findProductReservations(locationProductIds: Collection<UUID>): List<ProductReservationRow>
}
