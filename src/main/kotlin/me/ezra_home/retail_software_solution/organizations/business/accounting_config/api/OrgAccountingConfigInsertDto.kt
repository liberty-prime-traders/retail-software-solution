package me.ezra_home.retail_software_solution.organizations.business.accounting_config.api

import java.time.DayOfWeek

data class OrgAccountingConfigInsertDto(
    val fiscalYearEndMonth: Int,
    val fiscalYearEndDay: Int,
    val fiscalPeriodCycle: FiscalPeriodCycle,
    val periodWeekStartDay: DayOfWeek,
    val periodPrepDays: Int
)
