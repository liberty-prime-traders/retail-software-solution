package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import java.time.LocalDate

class SemiAnnualPeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, context: PeriodGenerationContext): PeriodRange {
        val start = lastPeriodEnd.plusDays(1)
        val endMonth = start.plusMonths(5)
        val end = endMonth.withDayOfMonth(endMonth.lengthOfMonth())
        return PeriodRange(start, end)
    }

    override fun nextCleanStart(from: LocalDate, context: PeriodGenerationContext): LocalDate {
        if (from.dayOfMonth == 1 && (from.monthValue - 1) % 6 == 0) return from
        val monthsToAdd = 6 - ((from.monthValue - 1) % 6)
        return from.withDayOfMonth(1).plusMonths(monthsToAdd.toLong())
    }
}
