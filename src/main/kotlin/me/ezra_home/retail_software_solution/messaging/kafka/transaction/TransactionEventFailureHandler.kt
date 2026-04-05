package me.ezra_home.retail_software_solution.messaging.kafka.transaction

import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryInventoryProcessor
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import org.springframework.stereotype.Service

@Service
class TransactionEventFailureHandler(
  private val purchaseDeliveryProcessor: PurchaseDeliveryInventoryProcessor
) {

  fun handle(event: TransactionEvent) {
    when (event) {
      is PurchaseDeliveredEvent -> purchaseDeliveryProcessor.markFailed(event.deliveryId)
      else -> {}
    }
  }
}
