package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import java.time.LocalDate

class FourFourFivePeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, context: PeriodGenerationContext): PeriodRange {
        val start = lastPeriodEnd.plusDays(1)
        val positionInPattern = context.existingPeriodsInCurrentYear % 3
        val weeks = if (positionInPattern == 2) 5L else 4L
        return PeriodRange(start, start.plusWeeks(weeks).minusDays(1))
    }

    override fun nextCleanStart(from: LocalDate, context: PeriodGenerationContext): LocalDate {
        val weekStart = context.config.periodWeekStartDay
        if (from.dayOfWeek == weekStart) return from
        var candidate = from
        while (candidate.dayOfWeek != weekStart) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }
}
