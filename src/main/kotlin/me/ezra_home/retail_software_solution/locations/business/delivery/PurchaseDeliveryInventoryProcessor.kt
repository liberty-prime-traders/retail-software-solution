package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.locations.model.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.model.StockMovementEntity
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.StockItemSourceService
import me.ezra_home.retail_software_solution.locations.business.stock.MovementType
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseDeliveryStatus
import me.ezra_home.retail_software_solution.organizations.business.inventory.StockItemSource
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.math.BigDecimal
import java.util.UUID

@Service
class PurchaseDeliveryInventoryProcessor(
  private val stockItemSourceService: StockItemSourceService,
  private val locationProductRepository: LocationProductRepository,
  private val stockEntryRepository: StockEntryRepository,
  private val stockMovementRepository: StockMovementRepository,
  private val deliveryRepository: PurchaseDeliveryRepository
) {

  @TransactionalOnLocationSchema
  fun processDelivery(event: PurchaseDeliveredEvent) {
    val sourceTypeId = stockItemSourceService.findSourceId(StockItemSource.PURCHASE)
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

    val productIds = event.lines.map { it.locationProductId }
    val previousBalances = stockMovementRepository.findLatestBalances(productIds)
      .associate { it.getLocationProductId() to it.getRemainingQuantity() }

    val movements = event.lines.map { line ->
      val entry = entriesByLineId[line.deliveryLineId]!!
      val newBalance = (previousBalances[line.locationProductId] ?: BigDecimal.ZERO) + line.quantityDelivered
      StockMovementEntity(
        stockEntryId = entry.getNullSafeId(),
        locationProductId = line.locationProductId,
        movementType = MovementType.PURCHASE_RECEIVED,
        movedQuantity = line.quantityDelivered,
        remainingQuantity = newBalance,
        referenceId = line.deliveryLineId
      )
    }
    stockMovementRepository.saveAll(movements)

    val unitCostByProductId = event.lines.associate { it.locationProductId to it.unitCost }
    val products = locationProductRepository.findAllById(unitCostByProductId.keys)
    products.forEach { it.lastPurchasePrice = unitCostByProductId[it.id] }
    locationProductRepository.saveAll(products)

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
