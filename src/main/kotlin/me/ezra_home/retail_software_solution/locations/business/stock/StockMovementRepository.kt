package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.stock.api.MovementType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface StockMovementRepository : JpaRepository<StockMovementEntity, UUID> {

  fun findByExternalReferenceNumberAndMovementType(
    externalReferenceNumber: String,
    movementType: MovementType
  ): List<StockMovementEntity>

  fun existsByExternalReferenceNumberAndMovementType(
    externalReferenceNumber: String,
    movementType: MovementType
  ): Boolean

  @Query("SELECT DISTINCT sm.externalReferenceNumber FROM StockMovementEntity sm WHERE sm.externalReferenceNumber IN :refs AND sm.movementType = :movementType")
  fun findPresentRefs(refs: Collection<String>, movementType: MovementType): Set<String>

  fun findByExternalReferenceNumberInAndMovementType(
    externalReferenceNumbers: Collection<String>,
    movementType: MovementType
  ): List<StockMovementEntity>

  fun findByLocationProductIdAndCreatedOnAfter(locationProductId: UUID, after: OffsetDateTime): List<StockMovementEntity>
}
