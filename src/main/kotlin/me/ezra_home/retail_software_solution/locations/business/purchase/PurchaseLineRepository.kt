package me.ezra_home.retail_software_solution.locations.business.purchase

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PurchaseLineRepository : JpaRepository<PurchaseLineEntity, UUID> {
  fun findByPurchaseId(purchaseId: UUID): List<PurchaseLineEntity>
  fun findByPurchaseIdIn(purchaseIds: Collection<UUID>): List<PurchaseLineEntity>

  @Query("SELECT l.locationProductId FROM PurchaseLineEntity l WHERE l.id = :purchaseLineId")
  fun findLocationProductIdById(purchaseLineId: UUID): UUID?
}
