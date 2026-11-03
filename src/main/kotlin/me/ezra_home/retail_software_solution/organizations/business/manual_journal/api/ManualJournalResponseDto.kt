package me.ezra_home.retail_software_solution.organizations.business.manual_journal.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountType
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ManualJournalResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val summary: String,
    val details: String?,
    val postingDate: LocalDate,
    val reversalOfReferenceNumber: String?,
    val reversedByReferenceNumber: String?,
    val lines: List<ManualJournalLineResponseDto>,
    val postedBy: String,
    val postedOn: OffsetDateTime
)

data class ManualJournalLineResponseDto(
    val referenceNumber: String,
    val accountCode: String,
    val accountName: String,
    val accountType: AccountType,
    val entryType: EntryType,
    val amount: BigDecimal,
    val lineMemo: String?
)
