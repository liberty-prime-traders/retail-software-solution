package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PurchaseDeliveryRepository : JpaRepository<PurchaseDeliveryEntity, UUID> {
  fun findByPurchaseIdIn(purchaseIds: List<UUID>): List<PurchaseDeliveryEntity>
}
