package me.ezra_home.retail_software_solution.messaging.kafka.catalog

import me.ezra_home.retail_software_solution.messaging.kafka.common.BaseEvent
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.util.model.TableName
import java.util.UUID

data class CatalogChangedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.OrgLevel,
    override val timestamp: java.time.Instant,
    override val correlationId: UUID?,
    val tableName: TableName,
    val entityId: UUID
): BaseEvent()
