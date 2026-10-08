package me.ezra_home.retail_software_solution.cross_tier.expense.kafka_handler

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentVoidedEvent
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExpensePaymentVoidedHandlerForKafka(
    private val expenseReissuerResolver: ExpenseReissuerResolver
) : EventReissueHandler {

    override val eventType = ExpensePaymentVoidedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        expenseReissuerResolver.current().reissuePaymentVoided(sourceDocumentId)
    }
}
