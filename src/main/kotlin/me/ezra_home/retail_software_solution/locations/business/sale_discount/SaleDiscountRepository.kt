package me.ezra_home.retail_software_solution.locations.business.sale_discount

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SaleDiscountRepository : JpaRepository<SaleDiscountEntity, UUID> {
    fun findBySaleId(saleId: UUID): List<SaleDiscountEntity>
    fun findBySaleIdIn(saleIds: List<UUID>): List<SaleDiscountEntity>
    fun findBySaleLineIdIn(saleLineIds: Collection<UUID>): List<SaleDiscountEntity>

    @Query("SELECT d.id FROM SaleDiscountEntity d WHERE d.saleId = :saleId AND d.id IN :ids")
    fun findIdsBySaleIdAndIdIn(saleId: UUID, ids: Collection<UUID>): List<UUID>
}
