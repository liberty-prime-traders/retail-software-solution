package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.stock.api.ProductReservationRow
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockReservationRepository : JpaRepository<StockReservationEntity, UUID> {
    fun deleteBySaleId(saleId: UUID)
    fun deleteBySaleLineIdIn(saleLineIds: List<UUID>)

    @Query(
        "SELECT new me.ezra_home.retail_software_solution.locations.business.stock.api.ProductReservationRow(" +
                "r.locationProductId, r.saleId, SUM(r.quantityReserved)) " +
                "FROM StockReservationEntity r " +
                "WHERE r.locationProductId IN :locationProductIds " +
                "GROUP BY r.locationProductId, r.saleId"
    )
    fun findProductReservations(locationProductIds: Collection<UUID>): List<ProductReservationRow>
}
