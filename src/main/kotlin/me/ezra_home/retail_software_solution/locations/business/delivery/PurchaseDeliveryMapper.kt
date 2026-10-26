package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredLineDto
import java.time.Instant
import java.util.UUID

object PurchaseDeliveryMapper {

  fun toEntity(purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto) = PurchaseDeliveryEntity(
    purchaseId = purchaseDeliveryCreateDto.purchaseId,
    deliveredAt = purchaseDeliveryCreateDto.deliveredAt,
    notes = purchaseDeliveryCreateDto.notes
  )

  fun toLineEntities(deliveryId: UUID, purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto, storageUnitsPerUnitByPurchaseLineId: Map<UUID, Long>) =
    purchaseDeliveryCreateDto.lines.map { lineDto ->
      PurchaseDeliveryLineEntity(
        purchaseDeliveryId = deliveryId,
        purchaseLineId = lineDto.purchaseLineId,
        userProvidedQuantityDelivered = lineDto.userProvidedQuantityDelivered.toBigDecimal(),
        unitCost = lineDto.unitCost.toBigDecimal(),
        unitId = lineDto.unitId,
        storageUnitsPerUnit = storageUnitsPerUnitByPurchaseLineId.getValue(lineDto.purchaseLineId)
      )
    }

  fun toEvent(
    purchaseId: UUID,
    supplierId: UUID,
    deliveryRecord: DeliveryRecord,
    purchaseLineById: Map<UUID, PurchaseLineDto>,
    sourceContext: EventSourceContext.LocationLevel
  ) = PurchaseDeliveredEvent(
    eventId = UUID.randomUUID(),
    sourceContext = sourceContext,
    timestamp = Instant.now(),
    correlationId = null,
    purchaseId = purchaseId,
    deliveryId = deliveryRecord.delivery.id!!,
    deliveryReferenceNumber = deliveryRecord.delivery.requiredReference(),
    deliveredAt = deliveryRecord.delivery.deliveredAt,
    supplierId = supplierId,
    lines = deliveryRecord.lines.map { dl ->
      val pl = purchaseLineById[dl.purchaseLineId]!!
      PurchaseDeliveredLineDto(
        deliveryLineId = dl.id!!,
        lineReferenceNumber = dl.requiredReference(),
        locationProductId = pl.locationProductId,
        userProvidedQuantityDelivered = dl.userProvidedQuantityDelivered,
        unitId = dl.unitId,
        storageUnitsPerUnit = dl.storageUnitsPerUnit,
        unitCost = dl.unitCost
      )
    },
    deliveryTotal = deliveryRecord.lines.sumOf { it.lineTotal() }
  )
}
