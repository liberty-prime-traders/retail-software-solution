package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodUtils
import java.time.LocalDate

class AnnualPeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, config: OrgAccountingConfigDto): PeriodRange {
        val start = lastPeriodEnd.plusDays(1)
        return PeriodRange(start, FiscalPeriodUtils.yearEnd(start, config.fiscalYearEndMonth))
    }

    override fun nextCleanStart(from: LocalDate, config: OrgAccountingConfigDto): LocalDate {
        val yearEnd = FiscalPeriodUtils.yearEnd(from, config.fiscalYearEndMonth)
        val yearStart = FiscalPeriodUtils.yearStart(yearEnd)
        return if (from == yearStart) from else yearEnd.plusDays(1)
    }
}
