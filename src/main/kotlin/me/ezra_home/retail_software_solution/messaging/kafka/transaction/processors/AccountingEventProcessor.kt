package me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService

abstract class AccountingEventProcessor<EVENT : TransactionEvent>(
    private val ledgerPostingService: LedgerPostingService
) : TransactionEventProcessor<EVENT> {

    open fun prepareLedgerRequest(event: EVENT): LedgerPostingRequest? = null

    open fun prepareLedgerRequests(event: EVENT): List<LedgerPostingRequest> =
        listOfNotNull(prepareLedgerRequest(event))

    override fun handle(event: EVENT) {
        prepareLedgerRequests(event).forEach { ledgerPostingService.post(it) }
    }
}
