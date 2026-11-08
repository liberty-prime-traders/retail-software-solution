package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCogsBackfillCandidate
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SaleLineRepository : JpaRepository<SaleLineEntity, UUID> {

    fun findBySaleId(saleId: UUID): List<SaleLineEntity>

    fun findByReferenceNumberIn(referenceNumbers: Collection<String>): List<SaleLineEntity>

    fun countByCogsTotalIsNotNull(): Long

    @Query(
        """
        SELECT NEW me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCogsBackfillCandidate(
            saleLine.id, saleLine.cogsTotal, sale.dateSold)
        FROM SaleLineEntity saleLine
        JOIN SaleEntity sale ON sale.id = saleLine.saleId
        WHERE saleLine.cogsTotal IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM SaleLineCogsEntity saleLineCogs WHERE saleLineCogs.saleLineId = saleLine.id)
        ORDER BY saleLine.id
        """
    )
    fun findLinesMissingCogsRecord(pageable: Pageable): List<SaleLineCogsBackfillCandidate>
}
