package me.ezra_home.retail_software_solution.organizations.business.accounting_config.api

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import java.time.DayOfWeek
import java.util.Optional

data class OrgAccountingConfigUpdateDto(
    val fiscalYearEndMonth: Optional<Int>? = null,
    val fiscalYearEndDay: Optional<Int>? = null,
    val fiscalPeriodCycle: Optional<FiscalPeriodCycle>? = null,
    val periodWeekStartDay: Optional<DayOfWeek>? = null,
    val periodPrepDays: Optional<Int>? = null
) {

    fun applyTo(existing: OrgAccountingConfigDto): OrgAccountingConfigDto = existing.copy(
        fiscalYearEndMonth = fiscalYearEndMonth?.orElseGet { existing.fiscalYearEndMonth } ?: existing.fiscalYearEndMonth,
        fiscalYearEndDay = fiscalYearEndDay?.orElseGet { existing.fiscalYearEndDay } ?: existing.fiscalYearEndDay,
        fiscalPeriodCycle = fiscalPeriodCycle?.orElseGet { existing.fiscalPeriodCycle } ?: existing.fiscalPeriodCycle,
        periodWeekStartDay = periodWeekStartDay?.orElseGet { existing.periodWeekStartDay } ?: existing.periodWeekStartDay,
        periodPrepDays = periodPrepDays?.orElseGet { existing.periodPrepDays } ?: existing.periodPrepDays
    )
}
