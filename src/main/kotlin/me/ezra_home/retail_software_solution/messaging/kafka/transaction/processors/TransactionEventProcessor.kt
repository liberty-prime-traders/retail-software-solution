package me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import kotlin.reflect.KClass

sealed interface TransactionEventProcessor<EVENT : TransactionEvent> {
    val eventType: KClass<EVENT>
    fun handle(event: EVENT)
    fun shouldProcess(event: EVENT): Boolean
}
