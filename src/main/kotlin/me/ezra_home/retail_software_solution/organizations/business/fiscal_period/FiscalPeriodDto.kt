package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class FiscalPeriodDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val yearEnd: Boolean,
    val stub: Boolean,
val closedAt: Instant?,
    val closedBy: UUID?
) {
    val isClosed get() = closedAt != null
}
