package me.ezra_home.retail_software_solution.organizations.business.accounting_config.api

import java.time.DayOfWeek
import java.time.OffsetDateTime
import java.util.UUID

data class OrgAccountingConfigDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val fiscalYearEndMonth: Int,
    val fiscalYearEndDay: Int,
    val fiscalPeriodCycle: FiscalPeriodCycle,
    val periodWeekStartDay: DayOfWeek,
    val periodPrepDays: Int
)
