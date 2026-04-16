package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryStatus
import me.ezra_home.retail_software_solution.locations.business.stock.api.PurchaseDeliveryStockUpdater
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.InventoryEventProcessor
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class PurchaseDeliveryInventoryProcessor(
    private val locationProductService: LocationProductService,
    private val purchaseDeliveryStockUpdater: PurchaseDeliveryStockUpdater,
    private val deliveryRepository: PurchaseDeliveryRepository
) : InventoryEventProcessor<PurchaseDeliveredEvent> {

  override val eventType: KClass<PurchaseDeliveredEvent> = PurchaseDeliveredEvent::class

  @TransactionalOnLocationSchema(readOnly = true)
  override fun shouldProcess(event: PurchaseDeliveredEvent): Boolean {
    return deliveryRepository.findById(event.deliveryId)
      .map { it.status != PurchaseDeliveryStatus.RECEIVED }
      .orElseGet { false }
  }

  @TransactionalOnLocationSchema
  override fun handle(event: PurchaseDeliveredEvent) {
    purchaseDeliveryStockUpdater.recordPurchaseDelivery(event)
    val unitCostByProductId = event.lines.associate { it.locationProductId to it.unitCost }
    locationProductService.updateLastPurchasePrices(unitCostByProductId)
    val delivery = deliveryRepository.findById(event.deliveryId).orElseThrow { UpdatingNonExistingRecordException() }
    delivery.status = PurchaseDeliveryStatus.RECEIVED
    deliveryRepository.save(delivery)
  }
}
