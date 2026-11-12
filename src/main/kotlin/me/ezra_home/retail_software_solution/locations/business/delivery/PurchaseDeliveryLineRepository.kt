package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryLineStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PurchaseDeliveryLineRepository : JpaRepository<PurchaseDeliveryLineEntity, UUID> {
  fun findByReferenceNumberIn(referenceNumbers: List<String>): List<PurchaseDeliveryLineEntity>

  fun findByPurchaseDeliveryIdIn(deliveryIds: List<UUID>): List<PurchaseDeliveryLineEntity>

  fun findByPurchaseDeliveryIdInAndStatus(
    deliveryIds: Collection<UUID>,
    status: PurchaseDeliveryLineStatus
  ): List<PurchaseDeliveryLineEntity>

  fun findByPurchaseLineIdAndStatus(
    purchaseLineId: UUID,
    status: PurchaseDeliveryLineStatus
  ): List<PurchaseDeliveryLineEntity>

  // Native, because purchase_line belongs to the purchase domain. Paged by location product id, one row per product.
  // Deleted with the last purchase cost backfill.
  @Query("""
    SELECT DISTINCT ON (pl.location_product_id)
      pl.location_product_id AS "locationProductId",
      dl.unit_cost AS "unitCost",
      dl.storage_units_per_user_provided_unit AS "storageUnitsPerUserProvidedUnit"
    FROM purchase_delivery_line dl
    JOIN purchase_delivery d ON d.id = dl.purchase_delivery_id
    JOIN purchase_line pl ON pl.id = dl.purchase_line_id
    WHERE d.status = :deliveryStatusCode AND dl.status = :lineStatusCode
      AND pl.location_product_id > :afterLocationProductId
    ORDER BY pl.location_product_id, d.delivered_at DESC, d.id DESC, dl.id DESC
    LIMIT :batchSize
  """, nativeQuery = true)
  fun findLastDeliveredCostsAfter(
    deliveryStatusCode: String,
    lineStatusCode: String,
    afterLocationProductId: UUID,
    batchSize: Int
  ): List<LastDeliveredCostProjection>

  @Query(
    "SELECT d.purchaseId AS purchaseId, l.purchaseLineId AS purchaseLineId " +
      "FROM PurchaseDeliveryEntity d, PurchaseDeliveryLineEntity l " +
      "WHERE l.id = :deliveryLineId AND d.id = l.purchaseDeliveryId"
  )
  fun findOwnersById(deliveryLineId: UUID): PurchaseDeliveryLineOwners?
}
