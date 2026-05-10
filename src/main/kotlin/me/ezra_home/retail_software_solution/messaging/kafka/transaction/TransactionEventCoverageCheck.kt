package me.ezra_home.retail_software_solution.messaging.kafka.transaction

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.platform.business.startup_checks.StartupCheck
import org.springframework.stereotype.Component

@Component
class TransactionEventCoverageCheck(
    private val reissueHandlers: List<EventReissueHandler>
) : StartupCheck {

    override val name = "transaction-event-reissue-coverage"

    override fun check() {
        val eventTypes = TransactionEvent::class.sealedSubclasses
        val handlerTypes = reissueHandlers.map { it.eventType }.toSet()
        val missing = eventTypes.filter { it !in handlerTypes }
        if (missing.isNotEmpty()) {
            throw IllegalStateException(
                "TransactionEvent subtypes missing EventReissueHandler: " +
                    missing.joinToString { it.simpleName ?: it.toString() }
            )
        }
    }
}
