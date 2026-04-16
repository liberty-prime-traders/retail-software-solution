package me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService

abstract class AccountingEventProcessor<EVENT : TransactionEvent>(
    private val ledgerPostingService: LedgerPostingService
) : TransactionEventProcessor<EVENT> {

    abstract fun prepareLedgerRequest(event: EVENT): LedgerPostingRequest

    override fun handle(event: EVENT) {
        ledgerPostingService.post(prepareLedgerRequest(event))
    }
}
