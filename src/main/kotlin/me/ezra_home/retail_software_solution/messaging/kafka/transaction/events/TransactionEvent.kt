package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.BaseEvent
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import java.util.UUID

sealed class TransactionEvent: BaseEvent() {
    abstract override val sourceContext: EventSourceContext
    abstract val sourceDocumentId: UUID
}
