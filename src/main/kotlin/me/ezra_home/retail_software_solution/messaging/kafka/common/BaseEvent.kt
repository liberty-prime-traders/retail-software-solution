package me.ezra_home.retail_software_solution.messaging.kafka.common

import java.time.Instant
import java.util.UUID

abstract class BaseEvent {
    abstract val eventId: UUID
    abstract val sourceSchema: String
    abstract val timestamp: Instant
    abstract val correlationId: UUID?
}
