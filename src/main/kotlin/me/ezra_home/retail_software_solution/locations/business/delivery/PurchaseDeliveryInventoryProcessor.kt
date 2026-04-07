package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryStatus
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockService
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSourceService
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Service
class PurchaseDeliveryInventoryProcessor(
  private val stockItemSourceService: StockItemSourceService,
  private val locationProductService: LocationProductService,
  private val stockService: StockService,
  private val deliveryRepository: PurchaseDeliveryRepository
) {

  @TransactionalOnLocationSchema
  fun processDelivery(event: PurchaseDeliveredEvent) {
    val sourceTypeId = stockItemSourceService.findSourceId(StockItemSource.PURCHASE)
    stockService.recordPurchaseDelivery(event, sourceTypeId)

    val unitCostByProductId = event.lines.associate { it.locationProductId to it.unitCost }
    locationProductService.updateLastPurchasePrices(unitCostByProductId)

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
