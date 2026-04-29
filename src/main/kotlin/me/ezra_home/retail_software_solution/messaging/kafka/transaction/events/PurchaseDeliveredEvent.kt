package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseDeliveredEvent(
  override val eventId: UUID,
  override val sourceContext: EventSourceContext.LocationLevel,
  override val timestamp: Instant,
  override val correlationId: UUID?,
  val purchaseId: UUID,
  val deliveryId: UUID,
  val deliveryReferenceNumber: String,
  val deliveredAt: OffsetDateTime,
  val supplierId: UUID,
  val lines: List<PurchaseDeliveredLineDto>
) : TransactionEvent() {
  override val sourceDocumentId: UUID get() = deliveryId
  val deliveryTotal: BigDecimal get() = lines.sumOf { Decimals.multiplyScale4(it.quantityDelivered, it.unitCost) }
}
