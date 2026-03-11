package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveredLineDto
import java.time.Instant
import java.util.UUID

data class PurchaseDeliveredEvent(
  override val eventId: UUID,
  override val sourceSchema: String,
  override val timestamp: Instant,
  override val correlationId: UUID?,
  val purchaseId: UUID,
  val deliveryId: UUID,
  val supplierId: UUID,
  val lines: List<PurchaseDeliveredLineDto>
) : TransactionEvent()
