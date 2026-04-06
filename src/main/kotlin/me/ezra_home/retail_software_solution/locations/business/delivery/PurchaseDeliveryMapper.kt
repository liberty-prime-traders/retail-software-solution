package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveredLineDto
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import java.time.Instant
import java.util.UUID

object PurchaseDeliveryMapper {

  fun toEntity(dto: PurchaseDeliveryCreateDto) = PurchaseDeliveryEntity(
    purchaseId = dto.purchaseId,
    deliveredAt = dto.deliveredAt,
    notes = dto.notes
  )

  fun toLineEntities(deliveryId: UUID, dto: PurchaseDeliveryCreateDto) =
    dto.lines.map { lineDto ->
      PurchaseDeliveryLineEntity(
        purchaseDeliveryId = deliveryId,
        purchaseLineId = lineDto.purchaseLineId,
        quantityDelivered = lineDto.quantityDelivered,
        unitCost = lineDto.unitCost
      )
    }

  fun toEvent(
    purchaseId: UUID,
    supplierId: UUID,
    deliveryRecord: DeliveryRecord,
    purchaseLineById: Map<UUID, PurchaseLineDto>,
    sourceSchema: String
  ) = PurchaseDeliveredEvent(
    eventId = UUID.randomUUID(),
    sourceSchema = sourceSchema,
    timestamp = Instant.now(),
    correlationId = null,
    purchaseId = purchaseId,
    deliveryId = deliveryRecord.delivery.getNullSafeId(),
    supplierId = supplierId,
    lines = deliveryRecord.lines.map { dl ->
      val pl = purchaseLineById[dl.purchaseLineId]!!
      PurchaseDeliveredLineDto(
        deliveryLineId = dl.getNullSafeId(),
        locationProductId = pl.locationProductId,
        quantityDelivered = dl.quantityDelivered,
        unitCost = dl.unitCost
      )
    }
  )
}
