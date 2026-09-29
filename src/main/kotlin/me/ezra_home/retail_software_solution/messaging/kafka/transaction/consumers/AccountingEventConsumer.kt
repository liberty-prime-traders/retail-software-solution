package me.ezra_home.retail_software_solution.messaging.kafka.transaction.consumers

import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class AccountingEventConsumer(
    private val accountingEventProcessors: List<AccountingEventProcessor<*>>,
    private val transactionEventConsumerSupport: TransactionEventConsumerSupport
) {

    @KafkaListener(
        topics = [KafkaConstants.Topics.TRANSACTION_EVENTS],
        groupId = KafkaConstants.ConsumerGroups.Transaction.ACCOUNTING
    )
    fun onTransactionEvent(event: TransactionEvent) {
        transactionEventConsumerSupport.consume(
            event,
            ServiceAccount.ACCOUNTING_PROCESSOR,
            KafkaConstants.ConsumerGroups.Transaction.ACCOUNTING,
            accountingEventProcessors
        )
    }
}
