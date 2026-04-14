package me.ezra_home.retail_software_solution.organizations.business.accounting_config.api

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.FiscalPeriodCycle
import java.time.DayOfWeek
import java.util.UUID

data class OrgAccountingConfigResponseDto(
    val id: UUID,
    val fiscalYearEndMonth: Int,
    val fiscalYearEndDay: Int,
    val fiscalPeriodCycle: FiscalPeriodCycle,
    val periodWeekStartDay: DayOfWeek,
    val periodPrepDays: Int
)
