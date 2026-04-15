package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodUtils
import java.time.LocalDate


class AnnualPeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, context: PeriodGenerationContext): PeriodRange {
        val start = lastPeriodEnd.plusDays(1)
        val config = context.config
        val end = FiscalPeriodUtils.yearEnd(start, config.fiscalYearEndMonth)
        return PeriodRange(start, end)
    }

    override fun nextCleanStart(from: LocalDate, context: PeriodGenerationContext): LocalDate {
        val config = context.config
        val yearEnd = FiscalPeriodUtils.yearEnd(from, config.fiscalYearEndMonth)
        val yearStart = FiscalPeriodUtils.yearStart(yearEnd)
        return if (from == yearStart) from else yearEnd.plusDays(1)
    }

}
