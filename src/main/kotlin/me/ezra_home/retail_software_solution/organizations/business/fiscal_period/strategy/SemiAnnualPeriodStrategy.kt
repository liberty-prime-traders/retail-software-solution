package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodUtils
import java.time.LocalDate

class SemiAnnualPeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, config: OrgAccountingConfigDto): PeriodRange {
        val start = lastPeriodEnd.plusDays(1)
        val endMonth = start.plusMonths(5)
        return PeriodRange(start, endMonth.withDayOfMonth(endMonth.lengthOfMonth()))
    }

    override fun nextCleanStart(from: LocalDate, config: OrgAccountingConfigDto): LocalDate {
        val fiscalStartMonth = FiscalPeriodUtils.fiscalStartMonth(config.fiscalYearEndMonth)
        val offset = ((from.monthValue - fiscalStartMonth) + 12) % 12
        if (from.dayOfMonth == 1 && offset % 6 == 0) return from
        val monthsToAdd = 6 - (offset % 6)
        return from.withDayOfMonth(1).plusMonths(monthsToAdd.toLong())
    }
}
