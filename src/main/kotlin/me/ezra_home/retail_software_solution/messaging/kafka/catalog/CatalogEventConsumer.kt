package me.ezra_home.retail_software_solution.messaging.kafka.catalog

import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
internal class CatalogEventConsumer(
    private val catalogEventHandler: CatalogEventHandler
) {

    @KafkaListener(
        topics = [KafkaConstants.Topics.CATALOG_EVENTS],
        groupId = KafkaConstants.ConsumerGroups.Catalog.SYNC
    )
    fun onCatalogEvent(event: CatalogChangedEvent) {
        catalogEventHandler.consume(event)
    }
}
