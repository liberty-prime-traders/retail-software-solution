package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountPosting
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntriesValidator
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
@TransactionalOnOrganizationSchema
class LedgerPostingService(
    private val groupRepository: LedgerEntryGroupRepository,
    private val entryRepository: LedgerEntryRepository,
    private val subledgerRepository: SubledgerEntryRepository,
    private val accountsAndLedgerLock: AccountsAndLedgerLock,
    private val fiscalPeriodService: FiscalPeriodService,
    private val accountService: AccountService,
) {

    fun post(ledgerPostingRequest: LedgerPostingRequest) {
        LedgerEntriesValidator.validate(ledgerPostingRequest.entries)
        accountsAndLedgerLock.acquire(ledgerPostingRequest)
        val accountPostings = ledgerPostingRequest.entries.map { ledgerEntryRequest ->
            AccountPosting(
                accountCode = ledgerEntryRequest.accountCode,
                amount = ledgerEntryRequest.amount,
                entryType = ledgerEntryRequest.entryType
            )
        }
        accountService.assertPostable(accountPostings)
        val group = saveLedgerGroup(ledgerPostingRequest)
        val groupReference = group.requiredReference()
        saveLedgerEntries(groupReference, ledgerPostingRequest)
        saveSubledgerEntries(groupReference, ledgerPostingRequest)
        accountService.patchBalances(accountPostings)
    }

    private fun saveLedgerGroup(ledgerPostingRequest: LedgerPostingRequest): LedgerEntryGroupEntity {
        val fiscalPeriodId = fiscalPeriodService.findOpenForDate(ledgerPostingRequest.postingDate)
            ?: throw RtsGenericException("No open fiscal period for ${ledgerPostingRequest.postingDate}")
        val group = LedgerEntryGroupEntity(
            sourceReferenceNumber = ledgerPostingRequest.sourceReferenceNumber,
            sourceType = ledgerPostingRequest.sourceType,
            sourceLocationId = SessionContextProvider.getLocationIdOrNull(),
            fiscalPeriodId = fiscalPeriodId,
            postedOn = Instant.now()
        )
        return groupRepository.save(group)
    }

    private fun saveLedgerEntries(
        groupReferenceNumber: String,
        ledgerPostingRequest: LedgerPostingRequest
    ) {
        entryRepository.saveAll(
            ledgerPostingRequest.entries.map { ledgerEntryRequest ->
                LedgerEntryEntity(
                    groupReferenceNumber = groupReferenceNumber,
                    accountCode = ledgerEntryRequest.accountCode,
                    entryType = ledgerEntryRequest.entryType,
                    amount = ledgerEntryRequest.amount
                )
            }
        )
    }

    private fun saveSubledgerEntries(groupReferenceNumber: String, ledgerPostingRequest: LedgerPostingRequest) {
        val contactReferenceNumbers = ledgerPostingRequest.subledgerEntries.map { it.contactReferenceNumber }.toSet()
        val latestByContact = subledgerRepository.findLatestForContacts(contactReferenceNumbers)
            .associateBy { it.contactReferenceNumber }

        subledgerRepository.saveAll(
            ledgerPostingRequest.subledgerEntries.map { subledgerEntryRequest ->
                SubledgerEntryEntity(
                    groupReferenceNumber = groupReferenceNumber,
                    contactReferenceNumber = subledgerEntryRequest.contactReferenceNumber,
                    payableAmount = subledgerEntryRequest.payableAmount,
                    receivableAmount = subledgerEntryRequest.receivableAmount,
                    runningPayable = (latestByContact[subledgerEntryRequest.contactReferenceNumber]?.runningPayable ?: BigDecimal.ZERO) + subledgerEntryRequest.payableAmount,
                    runningReceivable = (latestByContact[subledgerEntryRequest.contactReferenceNumber]?.runningReceivable ?: BigDecimal.ZERO) + subledgerEntryRequest.receivableAmount
                )
            }
        )
    }
}
