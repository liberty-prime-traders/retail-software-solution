package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.BaseEvent
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext

sealed class TransactionEvent: BaseEvent() {
    abstract override val sourceContext: EventSourceContext.LocationLevel
}
