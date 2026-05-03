package me.ezra_home.retail_software_solution.locations.business.sale

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
interface SaleLineRepository : JpaRepository<SaleLineEntity, UUID> {

    fun findBySaleIdIn(saleIds: Collection<UUID>): List<SaleLineEntity>

    fun findBySaleId(saleId: UUID): List<SaleLineEntity>

    @Query("SELECT COALESCE(SUM(l.quantity * l.unitPrice), 0) FROM SaleLineEntity l WHERE l.saleId = :saleId")
    fun sumSaleTotal(saleId: UUID): BigDecimal
}
