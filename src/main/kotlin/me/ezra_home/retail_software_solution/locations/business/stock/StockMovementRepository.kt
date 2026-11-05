package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockMovementRepository : JpaRepository<StockMovementEntity, UUID> {

  fun findByDocumentReferenceAndMovementType(
    documentReference: String,
    movementType: MovementType
  ): List<StockMovementEntity>

  @Query("" +
          "SELECT DISTINCT sm.detailReference " +
          "FROM StockMovementEntity sm" +
          " WHERE sm.documentReference = :documentReference " +
          "AND sm.detailReference IN :detailReferences " +
          "AND sm.movementType = :movementType"
  )
  fun findPresentDetailReferences(documentReference: String, detailReferences: Collection<String>, movementType: MovementType): Set<String>

  // A cancelled dispatch leaves its TRANSFER_OUT rows in place and adds a TRANSFER_CANCELLED row for each, so the net is what is still out.
  @Query("" +
          "SELECT COALESCE(SUM(CASE WHEN sm.movementType = :transferOut THEN 1 ELSE -1 END), 0) " +
          "FROM StockMovementEntity sm " +
          "WHERE sm.movementType IN (:transferOut, :transferCancelled) " +
          "AND sm.stockEntryId IN (" +
            "SELECT se.id " +
            "FROM StockEntryEntity se " +
            "WHERE se.documentReference = :documentReference " +
            "AND se.detailReference = :detailReference" +
          ")"
  )
  fun countNetTransferredOutOnEntriesOfSource(
    documentReference: String,
    detailReference: String,
    transferOut: MovementType,
    transferCancelled: MovementType
  ): Long

  fun findByLocationProductId(locationProductId: UUID, pageable: Pageable): List<StockMovementEntity>
}
