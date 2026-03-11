package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.model.PurchaseDeliveryLineEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PurchaseDeliveryLineRepository : JpaRepository<PurchaseDeliveryLineEntity, UUID> {
  fun findByPurchaseDeliveryIdIn(deliveryIds: List<UUID>): List<PurchaseDeliveryLineEntity>
}
