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

  // A cancelled dispatch leaves its TRANSFER_OUT rows in place and adds a TRANSFER_CANCELLED row for each, so the net
  // is what is still out.
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

  @Query("" +
          "SELECT COUNT(sm) " +
          "FROM StockMovementEntity sm " +
          "WHERE sm.movementType IN :movementTypes " +
          "AND sm.stockEntryId IN (" +
            "SELECT se.id " +
            "FROM StockEntryEntity se " +
            "WHERE se.documentReference = :documentReference " +
            "AND se.detailReference = :detailReference" +
          ")"
  )
  fun countMovementsOfTypesOnEntriesOfSource(
    documentReference: String,
    detailReference: String,
    movementTypes: Collection<MovementType>
  ): Long

  // Native, because the delivery and purchase tables belong to other domains. Paged by id, so a movement that never
  // matches (its delivery is gone) is passed over instead of rescanned. Deleted with the purchase movement context
  // backfill.
  @Query("""
    SELECT sm.id AS "stockMovementId", p.reference_number AS "purchaseReferenceNumber"
    FROM stock_movement sm
    JOIN purchase_delivery d ON d.reference_number = sm.document_reference
    JOIN purchase p ON p.id = d.purchase_id
    WHERE sm.movement_type = :movementTypeCode AND sm.context IS NULL AND sm.id > :afterStockMovementId
    ORDER BY sm.id
    LIMIT :batchSize
  """, nativeQuery = true)
  fun findMovementsOfTypeMissingPurchaseContext(
    movementTypeCode: String,
    afterStockMovementId: UUID,
    batchSize: Int
  ): List<PurchaseMovementContextProjection>

  @Query("""
    SELECT COUNT(*)
    FROM stock_movement sm
    JOIN purchase_delivery d ON d.reference_number = sm.document_reference
    WHERE sm.movement_type = :movementTypeCode AND sm.context IS NULL
  """, nativeQuery = true)
  fun countMovementsOfTypeMissingPurchaseContext(movementTypeCode: String): Long

  fun findByStockEntryId(stockEntryId: UUID): List<StockMovementEntity>

  fun findByLocationProductIdAndDocumentReferenceIn(
    locationProductId: UUID,
    documentReferences: Collection<String>
  ): List<StockMovementEntity>

  fun findByLocationProductId(locationProductId: UUID, pageable: Pageable): List<StockMovementEntity>
}
