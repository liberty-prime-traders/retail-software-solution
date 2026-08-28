package me.ezra_home.retail_software_solution.messaging.kafka.transaction

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.platform.business.startup_checks.api.StartupCheck
import org.springframework.stereotype.Component

@Component
class TransactionEventCoverageCheck(
    private val reissueHandlers: List<EventReissueHandler>
) : StartupCheck {

    override val name = "transaction-event-reissue-coverage"

    override fun check() {
        val sealedSimpleNames = TransactionEvent::class.sealedSubclasses
            .map { requireNotNull(it.simpleName) { "TransactionEvent subtype $it has no simpleName" } }
            .toSet()
        val handlersBySimpleName = reissueHandlers.groupBy { it.eventType.simpleName }

        val collisions = handlersBySimpleName.filter { it.value.size > 1 }
        if (collisions.isNotEmpty()) {
            throw IllegalStateException(
                "Multiple EventReissueHandler beans share an eventType.simpleName: " +
                    collisions.entries.joinToString { (name, handlers) ->
                        "$name -> ${handlers.map { it::class.simpleName }}"
                    }
            )
        }

        val missing = sealedSimpleNames - handlersBySimpleName.keys
        if (missing.isNotEmpty()) {
            throw IllegalStateException(
                "TransactionEvent subtypes missing EventReissueHandler (by simpleName): ${missing.joinToString()}"
            )
        }
    }
}
