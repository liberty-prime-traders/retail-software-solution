package me.ezra_home.retail_software_solution.locations.business.sale

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
interface SaleLineStockReservationRepository : JpaRepository<SaleLineStockReservationEntity, UUID> {
    fun deleteBySaleId(saleId: UUID)
    fun findBySaleId(saleId: UUID): List<SaleLineStockReservationEntity>
    fun deleteBySaleLineIdIn(saleLineIds: List<UUID>)

    @Query(
        "SELECT COALESCE(SUM(r.quantityReserved), 0) " +
                "FROM SaleLineStockReservationEntity r" +
                " WHERE r.saleLineId IN :saleLineIds"
    )
    fun sumQuantityReservedBySaleLineIdIn(saleLineIds: List<UUID>): BigDecimal

    @Query(
        "SELECT COALESCE(SUM(r.quantityReserved), 0) " +
                "FROM SaleLineStockReservationEntity r " +
                "WHERE r.locationProductId = :locationProductId"
    )
    fun sumQuantityReservedByLocationProductId(locationProductId: UUID): BigDecimal

    @Query(
        "SELECT r.locationProductId, SUM(r.quantityReserved) " +
                "FROM SaleLineStockReservationEntity r " +
                "WHERE r.locationProductId IN :ids " +
                "GROUP BY r.locationProductId"
    )
    fun sumQuantityReservedByLocationProductIdIn(ids: Collection<UUID>): List<Array<Any>>
}
