package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveredLineDto
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryEntity
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
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
    purchase: PurchaseEntity,
    deliveryRecord: DeliveryRecord,
    purchaseLineById: Map<UUID, PurchaseLineEntity>,
    sourceSchema: String
  ) = PurchaseDeliveredEvent(
    eventId = UUID.randomUUID(),
    sourceSchema = sourceSchema,
    timestamp = Instant.now(),
    correlationId = null,
    purchaseId = purchase.getNullSafeId(),
    deliveryId = deliveryRecord.delivery.getNullSafeId(),
    supplierId = purchase.supplierId,
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
