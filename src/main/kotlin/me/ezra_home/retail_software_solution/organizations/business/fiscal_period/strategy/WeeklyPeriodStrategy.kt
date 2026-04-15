package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.FiscalPeriodUtils
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class WeeklyPeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, context: PeriodGenerationContext): PeriodRange {
        val start = lastPeriodEnd.plusDays(1)
        val normalEnd = start.plusDays(6)
        val fiscalYearEnd = FiscalPeriodUtils.yearEnd(start, context.config.fiscalYearEndMonth)
        val daysToYearEnd = ChronoUnit.DAYS.between(normalEnd, fiscalYearEnd)
        val end = if (daysToYearEnd in 1..6) fiscalYearEnd else normalEnd
        return PeriodRange(start, end)
    }

    override fun nextCleanStart(from: LocalDate, context: PeriodGenerationContext): LocalDate {
        val weekStart = context.config.periodWeekStartDay
        if (from.dayOfWeek == weekStart) return from
        val daysUntilStart = (weekStart.value - from.dayOfWeek.value + 7) % 7
        return from.plusDays(daysUntilStart.toLong())
    }
}
