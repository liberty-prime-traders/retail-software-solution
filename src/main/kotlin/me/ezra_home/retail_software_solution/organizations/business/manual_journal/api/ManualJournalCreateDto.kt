package me.ezra_home.retail_software_solution.organizations.business.manual_journal.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import java.time.LocalDate

data class ManualJournalCreateDto(
    val summary: String,
    val details: String? = null,
    val postingDate: LocalDate? = null,
    val lines: List<ManualJournalLineCreateDto>
)

data class ManualJournalLineCreateDto(
    val accountCode: String,
    val entryType: EntryType,
    val amount: PositiveMoney,
    val lineMemo: String? = null
)
