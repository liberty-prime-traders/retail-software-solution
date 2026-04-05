package me.ezra_home.retail_software_solution.messaging.kafka.transaction

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryInventoryHandler
import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
internal class InventoryEventConsumer(
  private val purchaseDeliveryHandler: PurchaseDeliveryInventoryHandler,
  private val organizationCache: OrganizationCache,
  private val locationCache: LocationCache
) {

  @KafkaListener(
    topics = [KafkaConstants.Topics.TRANSACTION_EVENTS],
    groupId = KafkaConstants.ConsumerGroups.Transaction.INVENTORY
  )
  fun onTransactionEvent(event: TransactionEvent) {
    ServiceAccountContext.runWithServiceAccount(ServiceAccount.INVENTORY_PROCESSOR) {
      setupContext(event.sourceSchema)
      when (event) {
        is PurchaseDeliveredEvent -> purchaseDeliveryHandler.handle(event)
        else -> {}
      }
    }
  }

  private fun setupContext(locationSchema: String) {
    for (org in organizationCache.getAllOrganizations()) {
      SessionContextProvider.initOrganization(org)
      val location = locationCache.getAllLocations().find { it.schemaName == locationSchema }
      if (location != null) {
        SessionContextProvider.initLocation(location)
        return
      }
    }
    throw RtsGenericException("No location found for schema $locationSchema.")
  }
}
