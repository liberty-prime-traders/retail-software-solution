package me.ezra_home.retail_software_solution.organizations.business.loan.api

import me.ezra_home.retail_software_solution.util.business.StringUtils
import java.time.LocalDate
import java.time.OffsetDateTime

data class LoanSearchParameters(
    val createdFrom: OffsetDateTime? = null,
    val createdBefore: OffsetDateTime? = null,
    val lenderContactReferences: List<String> = emptyList(),
    val statuses: Set<LoanStatus> = emptySet(),
    val loanDateFrom: LocalDate? = null,
    val loanDateBefore: LocalDate? = null,
    val references: List<String> = emptyList()
) {

    fun sanitized(): LoanSearchParameters = copy(
        lenderContactReferences = StringUtils.dropBlank(lenderContactReferences),
        references = StringUtils.dropBlank(references)
    )
}
