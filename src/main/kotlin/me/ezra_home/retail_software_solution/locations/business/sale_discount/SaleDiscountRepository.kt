package me.ezra_home.retail_software_solution.locations.business.sale_discount

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SaleDiscountRepository : JpaRepository<SaleDiscountEntity, UUID> {
    fun findBySaleId(saleId: UUID): List<SaleDiscountEntity>
    fun findBySaleIdIn(saleIds: List<UUID>): List<SaleDiscountEntity>
    fun deleteBySaleLineIdIn(saleLineIds: Collection<UUID>)
    fun deleteByIdIn(ids: List<UUID>)
}
