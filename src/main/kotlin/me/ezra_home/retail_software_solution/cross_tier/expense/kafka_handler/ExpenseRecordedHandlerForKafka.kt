package me.ezra_home.retail_software_solution.cross_tier.expense.kafka_handler

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseRecordedEvent
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExpenseRecordedHandlerForKafka(
    private val expenseReissuerResolver: ExpenseReissuerResolver
) : EventReissueHandler {

    override val eventType = ExpenseRecordedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        expenseReissuerResolver.current().reissueRecorded(sourceDocumentId)
    }
}
