package me.ezra_home.retail_software_solution.organizations.business.loan.search

import java.time.OffsetDateTime
import java.util.UUID

data class LoanSearchRow(
    val id: UUID,
    val createdOn: OffsetDateTime
)
