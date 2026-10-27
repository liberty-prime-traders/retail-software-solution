package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import java.math.BigDecimal
import java.util.UUID

data class PurchaseDeliveredLineDto(
  val deliveryLineId: UUID,
  val lineReferenceNumber: String,
  val locationProductId: UUID,
  val userProvidedQuantityDelivered: BigDecimal,
  val unitId: UUID,
  val storageUnitsPerUserProvidedUnit: Long,
  val unitCost: BigDecimal
)
