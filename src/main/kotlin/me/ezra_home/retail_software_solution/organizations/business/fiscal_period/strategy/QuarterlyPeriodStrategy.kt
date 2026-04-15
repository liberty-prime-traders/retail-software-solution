package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import java.time.LocalDate

class QuarterlyPeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, context: PeriodGenerationContext): PeriodRange {
        val start = lastPeriodEnd.plusDays(1)
        val endMonth = start.plusMonths(2)
        val end = endMonth.withDayOfMonth(endMonth.lengthOfMonth())
        return PeriodRange(start, end)
    }

    override fun nextCleanStart(from: LocalDate, context: PeriodGenerationContext): LocalDate {
        val fiscalStartMonth = (context.config.fiscalYearEndMonth % 12) + 1
        val offset = ((from.monthValue - fiscalStartMonth) + 12) % 12
        if (from.dayOfMonth == 1 && offset % 3 == 0) return from
        val monthsToAdd = 3 - (offset % 3)
        return from.withDayOfMonth(1).plusMonths(monthsToAdd.toLong())
    }
}
