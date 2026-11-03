package me.ezra_home.retail_software_solution.organizations.business.manual_journal

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.api.ManualJournalLineCreateDto
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.api.ManualJournalResponseDto
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TransactionalOnOrganizationSchema
class ManualJournalRecorder(
    private val manualJournalRepository: ManualJournalRepository,
    private val manualJournalLineRepository: ManualJournalLineRepository,
    private val ledgerPostingService: LedgerPostingService,
    private val referenceNumberGenerator: ReferenceNumberGenerator,
    private val accountDataFetcher: AccountDataFetcher,
    private val manualJournalResponseMapper: ManualJournalResponseMapper
) {

    fun recordCreate(
        summary: String,
        details: String?,
        postingDate: LocalDate,
        manualJournalLineCreateDtos: List<ManualJournalLineCreateDto>
    ): ManualJournalResponseDto {
        val referenceNumber = referenceNumberGenerator.generateSingle(TableName.MANUAL_JOURNAL)
        val saved = saveHeader(referenceNumber, summary, details, postingDate, reversalOfManualJournal = null)
        val savedLines = saveLines(saved, manualJournalLineCreateDtos)
        postToLedger(referenceNumber, postingDate, manualJournalLineCreateDtos, reversal = false)
        return toResponse(saved, savedLines, reversalOfManualJournal = null)
    }

    fun recordReversal(original: ManualJournalEntity, reason: String): ManualJournalResponseDto {
        val referenceNumber = referenceNumberGenerator.generateSingle(TableName.MANUAL_JOURNAL)
        val postingDate = DateTimes.Local.Now.organization()
        val manualJournalLineCreateDtos = reversedLinesOf(original)
        val saved = saveHeader(referenceNumber, reason, details = null, postingDate, reversalOfManualJournal = original)
        val savedLines = saveLines(saved, manualJournalLineCreateDtos)
        postToLedger(referenceNumber, postingDate, manualJournalLineCreateDtos, reversal = true)
        return toResponse(saved, savedLines, reversalOfManualJournal = original)
    }

    private fun reversedLinesOf(original: ManualJournalEntity): List<ManualJournalLineCreateDto> =
        manualJournalLineRepository.findByManualJournalIdIn(listOf(original.requiredId()))
            .map { manualJournalLine ->
                ManualJournalLineCreateDto(
                    accountCode = manualJournalLine.accountCode,
                    entryType = manualJournalLine.entryType.opposite(),
                    amount = PositiveMoney.of(manualJournalLine.amount),
                    lineMemo = manualJournalLine.lineMemo
                )
            }

    private fun saveHeader(
        referenceNumber: String,
        summary: String,
        details: String?,
        postingDate: LocalDate,
        reversalOfManualJournal: ManualJournalEntity?
    ): ManualJournalEntity = manualJournalRepository.save(
        ManualJournalEntity(
            summary = summary,
            details = details,
            postingDate = postingDate,
            reversalOfManualJournalId = reversalOfManualJournal?.requiredId()
        ).also { it.referenceNumber = referenceNumber }
    )

    private fun saveLines(
        manualJournal: ManualJournalEntity,
        manualJournalLineCreateDtos: List<ManualJournalLineCreateDto>
    ): List<ManualJournalLineEntity> {
        val lineReferenceNumbers = referenceNumberGenerator.generateBulk(TableName.MANUAL_JOURNAL_LINE, manualJournalLineCreateDtos.size)
        return manualJournalLineRepository.saveAll(
            manualJournalLineCreateDtos.mapIndexed { lineIndex, manualJournalLineCreateDto ->
                ManualJournalLineEntity(
                    manualJournalId = manualJournal.requiredId(),
                    accountCode = manualJournalLineCreateDto.accountCode,
                    entryType = manualJournalLineCreateDto.entryType,
                    amount = manualJournalLineCreateDto.amount.toBigDecimal(),
                    lineMemo = manualJournalLineCreateDto.lineMemo
                ).also { it.referenceNumber = lineReferenceNumbers[lineIndex] }
            }
        )
    }

    private fun postToLedger(
        referenceNumber: String,
        postingDate: LocalDate,
        manualJournalLineCreateDtos: List<ManualJournalLineCreateDto>,
        reversal: Boolean
    ) {
        ledgerPostingService.post(
            LedgerPostingRequest(
                sourceReferenceNumber = referenceNumber,
                sourceType = if (reversal) LedgerSourceType.MANUAL_JOURNAL_VOID else LedgerSourceType.MANUAL_JOURNAL,
                postingDate = postingDate,
                entries = manualJournalLineCreateDtos.map { LedgerEntryRequest(it.accountCode, it.entryType, it.amount) },
                subledgerEntries = emptyList(),
                reversal = reversal
            )
        )
    }

    private fun toResponse(
        manualJournal: ManualJournalEntity,
        lines: List<ManualJournalLineEntity>,
        reversalOfManualJournal: ManualJournalEntity?
    ): ManualJournalResponseDto = manualJournalResponseMapper.toResponseDto(
        manualJournal = manualJournal,
        lines = lines.sortedBy { it.accountCode },
        reversalOfReferenceNumber = reversalOfManualJournal?.requiredReference(),
        reversedByReferenceNumber = null,
        accountsByCode = accountDataFetcher.getNamesAndTypesByCodes(lines.map { it.accountCode }.toSet())
    )
}
