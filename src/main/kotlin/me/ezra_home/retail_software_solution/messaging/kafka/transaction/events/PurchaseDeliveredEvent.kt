package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PurchaseDeliveredEvent(
  override val eventId: UUID,
  override val sourceSchema: String,
  override val timestamp: Instant,
  override val correlationId: UUID?,
  val purchaseId: UUID,
  val deliveryId: UUID,
  val deliveryReferenceNumber: String,
  val deliveredAt: Instant,
  val supplierId: UUID,
  val lines: List<PurchaseDeliveredLineDto>
) : TransactionEvent() {
  val deliveryTotal: BigDecimal get() = lines.sumOf { it.quantityDelivered.multiply(it.unitCost) }
}
