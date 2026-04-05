package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PurchaseDeliveryInventoryHandler(
  private val processor: PurchaseDeliveryInventoryProcessor
) {

  private val log = LoggerFactory.getLogger(PurchaseDeliveryInventoryHandler::class.java)

  fun handle(event: PurchaseDeliveredEvent) {
    try {
      processor.processDelivery(event)
    } catch (e: Exception) {
      log.error("Failed to process delivery event ${event.eventId} for delivery ${event.deliveryId}", e)
      processor.markFailed(event.deliveryId)
    }
  }
}
