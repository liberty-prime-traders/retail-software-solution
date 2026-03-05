package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.locations.model.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.model.StockMovementEntity
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.organizations.business.inventory.StockItemSourceRepository
import me.ezra_home.retail_software_solution.util.enums.MovementType
import me.ezra_home.retail_software_solution.util.enums.PurchaseDeliveryStatus
import me.ezra_home.retail_software_solution.util.enums.StockItemSource
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Service
class PurchaseDeliveryInventoryProcessor(
  private val stockItemSourceRepository: StockItemSourceRepository,
  private val stockEntryRepository: StockEntryRepository,
  private val stockMovementRepository: StockMovementRepository,
  private val deliveryRepository: PurchaseDeliveryRepository
) {

  @TransactionalOnLocationSchema
  fun processDelivery(event: PurchaseDeliveredEvent) {
    val sourceTypeId = stockItemSourceRepository.findByCode(StockItemSource.PURCHASE)?.id
      ?: throw RtsGenericException("Stock item source 'PURCHASE' not found.")

    val entriesByLineId = event.lines.associate { line ->
      line.deliveryLineId to StockEntryEntity(
        purchaseDeliveryLineId = line.deliveryLineId,
        locationProductId = line.locationProductId,
        sourceTypeId = sourceTypeId,
        batchSize = line.quantityDelivered,
        quantityRemaining = line.quantityDelivered,
        unitCost = line.unitCost,
        priority = 0
      )
    }
    stockEntryRepository.saveAll(entriesByLineId.values)

    val movements = event.lines.map { line ->
      val entry = entriesByLineId[line.deliveryLineId]!!
      val totalRemaining = stockEntryRepository.sumQuantityRemainingByLocationProductId(line.locationProductId)
      StockMovementEntity(
        stockEntryId = entry.id!!,
        locationProductId = line.locationProductId,
        movementType = MovementType.PURCHASE_RECEIVED,
        movedQuantity = line.quantityDelivered,
        remainingQuantity = totalRemaining,
        referenceId = line.deliveryLineId
      )
    }
    stockMovementRepository.saveAll(movements)

    val delivery = deliveryRepository.findById(event.deliveryId).orElseThrow { UpdatingNonExistingRecordException() }
    delivery.status = PurchaseDeliveryStatus.RECEIVED
    deliveryRepository.save(delivery)
  }

  @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
  fun markFailed(purchaseDeliveryId: UUID) {
    val delivery = deliveryRepository.findById(purchaseDeliveryId).orElseThrow { UpdatingNonExistingRecordException() }
    delivery.status = PurchaseDeliveryStatus.FAILED
    deliveryRepository.save(delivery)
  }
}
