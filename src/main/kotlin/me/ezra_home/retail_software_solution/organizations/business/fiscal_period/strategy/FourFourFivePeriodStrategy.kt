package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodUtils
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class FourFourFivePeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, config: OrgAccountingConfigDto): PeriodRange {
        val start = lastPeriodEnd.plusDays(1)
        val fyStart = FiscalPeriodUtils.yearStart(FiscalPeriodUtils.yearEnd(start, config.fiscalYearEndMonth))
        val firstPeriodStart = fyStart.plusDays(
            FiscalPeriodUtils.daysUntilDayOfWeek(fyStart, config.periodWeekStartDay).toLong()
        )
        val leadingDays = if (start.isBefore(firstPeriodStart))
            ChronoUnit.DAYS.between(start, firstPeriodStart)
        else 0L
        val weeks = if (FiscalPeriodUtils.positionIn445(firstPeriodStart, start) == 2)
            5L else 4L
        return PeriodRange(start, start.plusDays(leadingDays + weeks * 7 - 1))
    }

    override fun nextCleanStart(from: LocalDate, config: OrgAccountingConfigDto): LocalDate = from
}
