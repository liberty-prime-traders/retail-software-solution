package me.ezra_home.retail_software_solution.messaging.kafka.catalog

import me.ezra_home.retail_software_solution.messaging.kafka.common.BaseEvent

data class CatalogChangedEvent(
    override val eventId: java.util.UUID,
    override val eventType: String,
    override val sourceSchema: String,
    override val timestamp: java.time.Instant,
    override val correlationId: java.util.UUID?,
    val catalogEntityType: CatalogEntityType,
    val entityId: Long,
    val operation: CatalogOperation
): BaseEvent()
