package me.ezra_home.retail_software_solution.messaging.kafka.catalog

import me.ezra_home.retail_software_solution.messaging.kafka.common.BaseEvent
import java.util.UUID

data class CatalogChangedEvent(
    override val eventId: UUID,
    override val sourceSchema: String,
    override val timestamp: java.time.Instant,
    override val correlationId: UUID?,
    val catalogEntityType: CatalogEntityType,
    val entityId: UUID,
    val operation: CatalogOperation
): BaseEvent()
