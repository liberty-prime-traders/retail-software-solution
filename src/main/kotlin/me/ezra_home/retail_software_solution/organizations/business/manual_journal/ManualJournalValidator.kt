package me.ezra_home.retail_software_solution.organizations.business.manual_journal

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountSelectionCandidate
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account_selection.api.AccountSelectionRule
import me.ezra_home.retail_software_solution.organizations.business.manual_journal.api.ManualJournalLineCreateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import java.math.BigDecimal

object ManualJournalValidator {

    private const val MINIMUM_LINE_COUNT = 2

    fun createErrors(
        summary: String,
        lines: List<ManualJournalLineCreateDto>,
        candidatesByCode: Map<String, AccountSelectionCandidate>
    ): List<String> {
        val errors = mutableListOf<String>()
        if (StringUtils.getValueOrNull(summary) == null) errors += "Summary is required."
        if (lines.size < MINIMUM_LINE_COUNT) errors += "At least two lines are required."
        lines.map { it.accountCode }.distinct().mapNotNullTo(errors) { accountCode ->
            accountError(accountCode, candidatesByCode[accountCode])
        }
        if (totalOf(lines, EntryType.DEBIT).compareTo(totalOf(lines, EntryType.CREDIT)) != 0) errors += "Debits must equal credits."
        return errors
    }

    fun reversalErrors(reason: String, isReversal: Boolean, alreadyReversed: Boolean): List<String> = listOfNotNull(
        "Reason is required.".takeIf { StringUtils.getValueOrNull(reason) == null },
        "A reversal cannot be reversed.".takeIf { isReversal },
        "This manual journal has already been reversed.".takeIf { alreadyReversed }
    )

    private fun accountError(accountCode: String, candidate: AccountSelectionCandidate?): String? = when {
        candidate == null || !candidate.accountIsActive || !candidate.isLeaf -> "Account $accountCode is not postable."
        else -> AccountSelectionRule.MANUAL_JOURNAL.rejectionReason(candidate)
    }

    private fun totalOf(lines: List<ManualJournalLineCreateDto>, entryType: EntryType): BigDecimal =
        lines.filter { it.entryType == entryType }
            .fold(BigDecimal.ZERO) { total, manualJournalLineCreateDto -> total + manualJournalLineCreateDto.amount.toBigDecimal() }
}
