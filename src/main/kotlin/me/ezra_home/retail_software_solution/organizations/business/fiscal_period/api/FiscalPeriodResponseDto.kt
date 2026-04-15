package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class FiscalPeriodResponseDto(
    val id: UUID,
    val name: String,
    val fiscalYear: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val closable: Boolean,
    val yearEnd: Boolean,
    val stub: Boolean,
    val closedAt: Instant?,
    val closedBy: String?
)
