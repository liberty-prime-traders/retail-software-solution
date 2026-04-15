package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodUtils
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class WeeklyPeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, config: OrgAccountingConfigDto): PeriodRange {
        val start = lastPeriodEnd.plusDays(1)
        val daysToFirstBoundary = FiscalPeriodUtils.daysUntilDayOfWeek(start, config.periodWeekStartDay)
        val normalEnd = start.plusDays(daysToFirstBoundary + 6L)
        val fiscalYearEnd = FiscalPeriodUtils.yearEnd(start, config.fiscalYearEndMonth)
        val daysToYearEnd = ChronoUnit.DAYS.between(normalEnd, fiscalYearEnd)
        val end = if (daysToYearEnd in 1..6) fiscalYearEnd else normalEnd
        return PeriodRange(start, end)
    }

    override fun nextCleanStart(from: LocalDate, config: OrgAccountingConfigDto): LocalDate = from
}
