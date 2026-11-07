package me.ezra_home.retail_software_solution.locations.business.delivery

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PurchaseDeliveryLineRepository : JpaRepository<PurchaseDeliveryLineEntity, UUID> {
  fun findByPurchaseDeliveryIdIn(deliveryIds: List<UUID>): List<PurchaseDeliveryLineEntity>

  @Query(
    "SELECT d.purchaseId FROM PurchaseDeliveryEntity d, PurchaseDeliveryLineEntity l " +
      "WHERE l.id = :deliveryLineId AND d.id = l.purchaseDeliveryId"
  )
  fun findPurchaseIdById(deliveryLineId: UUID): UUID?

  @Query("SELECT l.purchaseLineId FROM PurchaseDeliveryLineEntity l WHERE l.id = :deliveryLineId")
  fun findPurchaseLineIdById(deliveryLineId: UUID): UUID?
}
