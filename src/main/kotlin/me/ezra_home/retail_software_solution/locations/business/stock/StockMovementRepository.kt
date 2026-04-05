package me.ezra_home.retail_software_solution.locations.business.stock

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockMovementRepository : JpaRepository<StockMovementEntity, UUID> {

  @Query("""
    SELECT sm.locationProductId AS locationProductId, sm.remainingQuantity AS remainingQuantity
    FROM StockMovementEntity sm
    WHERE sm.locationProductId IN :ids
    AND sm.createdOn = (
      SELECT MAX(sm2.createdOn) FROM StockMovementEntity sm2 WHERE sm2.locationProductId = sm.locationProductId
    )
  """)
  fun findLatestBalances(ids: Collection<UUID>): List<StockBalanceProjection>
}
