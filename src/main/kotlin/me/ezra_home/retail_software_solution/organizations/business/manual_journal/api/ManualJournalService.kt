package me.ezra_home.retail_software_solution.organizations.business.manual_journal.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountStructureLock
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.ManualJournalRecorder
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.ManualJournalRepository
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.ManualJournalValidator
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class ManualJournalService(
    private val manualJournalRepository: ManualJournalRepository,
    private val manualJournalRecorder: ManualJournalRecorder,
    private val accountDataFetcher: AccountDataFetcher,
    private val accountStructureLock: AccountStructureLock,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock,
    private val fiscalPeriodService: FiscalPeriodService
) {

    fun create(manualJournalCreateDto: ManualJournalCreateDto): ManualJournalResponseDto {
        val manualJournalLineCreateDtos = manualJournalCreateDto.lines.map { manualJournalLineCreateDto ->
            manualJournalLineCreateDto.copy(lineMemo = StringUtils.getValueOrNull(manualJournalLineCreateDto.lineMemo))
        }
        val postingDate = manualJournalCreateDto.postingDate ?: DateTimes.Local.Now.organization()
        DateTimes.Local.requireNotInFuture(postingDate, "Posting date")
        accountStructureLock.acquire(manualJournalLineCreateDtos.map { it.accountCode })
        val candidatesByCode = accountDataFetcher.getFreshSelectionCandidates().associateBy { it.code }
        val errors = buildList {
            addAll(ManualJournalValidator.createErrors(manualJournalCreateDto.summary, manualJournalLineCreateDtos, candidatesByCode))
            if (fiscalPeriodService.findOpenForDate(postingDate) == null) add("No open fiscal period for $postingDate.")
        }
        if (errors.isNotEmpty()) throw RtsGenericException(errors.joinToString(" "), payload = errors)
        return manualJournalRecorder.recordCreate(
            summary = manualJournalCreateDto.summary,
            details = StringUtils.getValueOrNull(manualJournalCreateDto.details),
            postingDate = postingDate,
            manualJournalLineCreateDtos = manualJournalLineCreateDtos
        )
    }

    fun reverse(referenceNumber: String, manualJournalReversalCreateDto: ManualJournalReversalCreateDto): ManualJournalResponseDto {
        val original = manualJournalRepository.findByReferenceNumber(referenceNumber)
            ?: throw RtsGenericException("Manual journal $referenceNumber not found")
        orgEntityAdvisoryLock.acquire(LockNamespaces.MANUAL_JOURNAL, original.requiredId().toString())
        val errors = ManualJournalValidator.reversalErrors(
            reason = manualJournalReversalCreateDto.reason,
            isReversal = original.reversalOfManualJournalId != null,
            alreadyReversed = manualJournalRepository.existsByReversalOfManualJournalId(original.requiredId())
        )
        if (errors.isNotEmpty()) throw RtsGenericException(errors.joinToString(" "), payload = errors)
        return manualJournalRecorder.recordReversal(original, manualJournalReversalCreateDto.reason)
    }
}
