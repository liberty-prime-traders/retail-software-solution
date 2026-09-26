package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.OpeningStockDeclaredEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class OpeningStockAccountingProcessor(
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<OpeningStockDeclaredEvent>(ledgerPostingService) {

    override val eventType: KClass<OpeningStockDeclaredEvent> = OpeningStockDeclaredEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: OpeningStockDeclaredEvent): Boolean =
        ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId(
            event.ledgerSourceReferenceNumber,
            SessionContextProvider.getLocationId()
        ).not()

    override fun prepareLedgerRequest(event: OpeningStockDeclaredEvent): LedgerPostingRequest {
        val amount = Decimals.multiplyScale4(event.quantity, event.unitCost)
        return LedgerPostingRequest(
            sourceReferenceNumber = event.ledgerSourceReferenceNumber,
            sourceType = LedgerSourceType.OPENING_STOCK,
            postingDate = event.postingDate,
            entries = listOf(
                LedgerEntryRequest(SystemAccount.INVENTORY.code, EntryType.DEBIT, amount),
                LedgerEntryRequest(SystemAccount.OPENING_BALANCE_EQUITY.code, EntryType.CREDIT, amount)
            ),
            subledgerEntries = emptyList()
        )
    }
}
