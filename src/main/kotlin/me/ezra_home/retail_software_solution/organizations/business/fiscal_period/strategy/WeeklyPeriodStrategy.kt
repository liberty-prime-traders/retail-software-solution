package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import java.time.LocalDate

class WeeklyPeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, context: PeriodGenerationContext): PeriodRange {
        val start = lastPeriodEnd.plusDays(1)
        return PeriodRange(start, start.plusDays(6))
    }

    override fun nextCleanStart(from: LocalDate, context: PeriodGenerationContext): LocalDate {
        val weekStart = context.config.periodWeekStartDay
        if (from.dayOfWeek == weekStart) return from
        val daysUntilStart = (weekStart.value - from.dayOfWeek.value + 7) % 7
        return from.plusDays(daysUntilStart.toLong())
    }
}
