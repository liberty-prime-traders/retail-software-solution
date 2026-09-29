package me.ezra_home.retail_software_solution.locations.business.sale_adjustment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SaleAdjustmentRepository : JpaRepository<SaleAdjustmentEntity, UUID> {
    fun findBySaleId(saleId: UUID): List<SaleAdjustmentEntity>
    fun findBySaleIdIn(saleIds: List<UUID>): List<SaleAdjustmentEntity>
    fun findBySaleLineIdIn(saleLineIds: Collection<UUID>): List<SaleAdjustmentEntity>

    @Query("SELECT a.id FROM SaleAdjustmentEntity a WHERE a.saleId = :saleId AND a.id IN :ids")
    fun findIdsBySaleIdAndIdIn(saleId: UUID, ids: Collection<UUID>): List<UUID>
}
