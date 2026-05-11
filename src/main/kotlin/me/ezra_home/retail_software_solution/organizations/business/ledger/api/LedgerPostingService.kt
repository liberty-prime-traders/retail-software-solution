package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
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
    private val fiscalPeriodService: FiscalPeriodService,
    private val accountService: AccountService,
) {

    fun post(request: LedgerPostingRequest) {
        LedgerEntriesValidator.validate(request.entries)
        val group = saveLedgerGroup(request)
        val groupReference = group.requiredReference()
        val entries = saveLedgerEntries(groupReference, request)
        saveSubledgerEntries(groupReference, request)
        patchRunningBalances(entries)
    }

    private fun saveLedgerGroup(request: LedgerPostingRequest): LedgerEntryGroupEntity {
        val fiscalPeriodId = fiscalPeriodService.findOpenForDate(request.postingDate)
            ?: throw RtsGenericException("No open fiscal period for ${request.postingDate}")
        val group = LedgerEntryGroupEntity(
            sourceReferenceNumber = request.sourceReferenceNumber,
            sourceType = request.sourceType,
            sourceLocationId = SessionContextProvider.getLocationId(),
            fiscalPeriodId = fiscalPeriodId,
            postedOn = Instant.now()
        )
        return groupRepository.save(group)
    }

    private fun saveLedgerEntries(
        groupReferenceNumber: String,
        request: LedgerPostingRequest
    ): List<LedgerEntryEntity> {
        return entryRepository.saveAll(
            request.entries.map { e ->
                LedgerEntryEntity(
                    groupReferenceNumber = groupReferenceNumber,
                    accountCode = e.accountCode,
                    entryType = e.entryType,
                    amount = e.amount
                )
            }
        )
    }

    private fun saveSubledgerEntries(groupReferenceNumber: String, request: LedgerPostingRequest) {
        val contactRefs = request.subledgerEntries.map { it.contactReferenceNumber }.toSet()
        val latestByContact = subledgerRepository.findLatestForContacts(contactRefs)
            .associateBy { it.contactReferenceNumber }

        subledgerRepository.saveAll(
            request.subledgerEntries.map { sub ->
                SubledgerEntryEntity(
                    groupReferenceNumber = groupReferenceNumber,
                    contactReferenceNumber = sub.contactReferenceNumber,
                    payableAmount = sub.payableAmount,
                    receivableAmount = sub.receivableAmount,
                    runningPayable = (latestByContact[sub.contactReferenceNumber]?.runningPayable ?: BigDecimal.ZERO) + sub.payableAmount,
                    runningReceivable = (latestByContact[sub.contactReferenceNumber]?.runningReceivable ?: BigDecimal.ZERO) + sub.receivableAmount
                )
            }
        )
    }

    private fun patchRunningBalances(ledgerEntries: List<LedgerEntryEntity>) {
        val summaryDtos = ledgerEntries.map { entry ->
            LedgerEntrySummaryDto(
                accountCode = entry.accountCode,
                entryType = entry.entryType,
                amount = entry.amount
            )
        }
        accountService.patchBalances(summaryDtos)
    }
}
