package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.OpeningBalanceUpsertedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class OpeningBalanceAccountingProcessor(
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<OpeningBalanceUpsertedEvent>(ledgerPostingService) {

    override val eventType: KClass<OpeningBalanceUpsertedEvent> = OpeningBalanceUpsertedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: OpeningBalanceUpsertedEvent): Boolean =
        ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationIdIsNull(
            event.ledgerSourceReferenceNumber
        ).not()

    override fun prepareLedgerRequest(event: OpeningBalanceUpsertedEvent): LedgerPostingRequest {
        val offsettingEntryType = event.accountEntryType.opposite()
        return LedgerPostingRequest(
            sourceReferenceNumber = event.ledgerSourceReferenceNumber,
            sourceType = LedgerSourceType.OPENING_BALANCE,
            postingDate = event.postingDate,
            entries = listOf(
                LedgerEntryRequest(event.accountCode, event.accountEntryType, event.amount),
                LedgerEntryRequest(SystemAccount.OPENING_BALANCE_EQUITY.code, offsettingEntryType, event.amount)
            ),
            subledgerEntries = emptyList()
        )
    }
}
