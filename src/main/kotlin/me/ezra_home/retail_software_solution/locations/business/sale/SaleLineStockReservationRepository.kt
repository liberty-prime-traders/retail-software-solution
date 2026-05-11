package me.ezra_home.retail_software_solution.locations.business.sale

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SaleLineStockReservationRepository : JpaRepository<SaleLineStockReservationEntity, UUID> {
    fun deleteBySaleId(saleId: UUID)
    fun deleteBySaleLineIdIn(saleLineIds: List<UUID>)

    @Query(
        "SELECT new me.ezra_home.retail_software_solution.locations.business.sale.ProductSaleReservationRow(" +
                "r.locationProductId, r.saleId, SUM(r.quantityReserved)) " +
                "FROM SaleLineStockReservationEntity r " +
                "WHERE r.locationProductId IN :ids " +
                "GROUP BY r.locationProductId, r.saleId"
    )
    fun findProductSaleReservations(ids: Collection<UUID>): List<ProductSaleReservationRow>

    @Query(
        value = "SELECT pg_advisory_xact_lock(hashtextextended(CAST(:productId AS text), 0))",
        nativeQuery = true
    )
    fun acquireProductReservationLock(productId: UUID)
}
