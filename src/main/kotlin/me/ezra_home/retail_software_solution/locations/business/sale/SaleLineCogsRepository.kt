package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCogsStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
interface SaleLineCogsRepository : JpaRepository<SaleLineCogsEntity, UUID> {

    @Query(
        """
        SELECT COALESCE(SUM(saleLineCogs.cogsTotal), 0)
        FROM SaleLineCogsEntity saleLineCogs
        JOIN SaleLineEntity saleLine ON saleLine.id = saleLineCogs.saleLineId
        WHERE saleLine.saleId = :saleId AND saleLineCogs.status = :status
        """
    )
    fun sumCogsTotalBySaleIdAndStatus(
        @Param("saleId") saleId: UUID,
        @Param("status") status: SaleLineCogsStatus,
    ): BigDecimal
}
