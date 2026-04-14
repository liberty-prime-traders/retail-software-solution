package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import java.time.LocalDate
import java.time.YearMonth

class MonthlyPeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, context: PeriodGenerationContext): PeriodRange {
        val month = YearMonth.from(lastPeriodEnd).plusMonths(1)
        return PeriodRange(month.atDay(1), month.atEndOfMonth())
    }

    override fun nextCleanStart(from: LocalDate, context: PeriodGenerationContext): LocalDate {
        if (from.dayOfMonth == 1) return from
        return from.withDayOfMonth(1).plusMonths(1)
    }
}
