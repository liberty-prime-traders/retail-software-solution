package me.ezra_home.retail_software_solution.organizations.business.manual_journal.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.ManualJournalLineRepository
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.ManualJournalRepository
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.ManualJournalResponseMapper
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class ManualJournalDataFetcher(
    private val manualJournalRepository: ManualJournalRepository,
    private val manualJournalLineRepository: ManualJournalLineRepository,
    private val accountDataFetcher: AccountDataFetcher,
    private val manualJournalResponseMapper: ManualJournalResponseMapper
) {

    fun getAll(): List<ManualJournalResponseDto> {
        val manualJournals = manualJournalRepository.findTop500ByOrderByCreatedOnDesc()
        val originalReferenceNumbersById = manualJournalRepository
            .findAllById(manualJournals.mapNotNull { it.reversalOfManualJournalId })
            .associate { it.requiredId() to it.requiredReference() }
        val reversedByReferenceNumbersById = manualJournals
            .mapNotNull { manualJournal -> manualJournal.reversalOfManualJournalId?.let { it to manualJournal.requiredReference() } }
            .toMap()
        val linesByManualJournalId = manualJournalLineRepository
            .findByManualJournalIdIn(manualJournals.map { it.requiredId() })
            .groupBy { it.manualJournalId }
        val accountsByCode = accountDataFetcher.getNamesAndTypesByCodes(
            linesByManualJournalId.values.flatten().map { it.accountCode }.toSet()
        )
        return manualJournals.map { manualJournal ->
            manualJournalResponseMapper.toResponseDto(
                manualJournal = manualJournal,
                lines = linesByManualJournalId.getValue(manualJournal.requiredId()).sortedBy { it.accountCode },
                reversalOfReferenceNumber = manualJournal.reversalOfManualJournalId?.let { originalReferenceNumbersById.getValue(it) },
                reversedByReferenceNumber = reversedByReferenceNumbersById[manualJournal.requiredId()],
                accountsByCode = accountsByCode
            )
        }
    }
}
