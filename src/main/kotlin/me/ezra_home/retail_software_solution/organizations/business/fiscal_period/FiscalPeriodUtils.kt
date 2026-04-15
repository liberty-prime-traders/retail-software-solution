package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object FiscalPeriodUtils {

    fun yearEnd(forDate: LocalDate, endMonth: Int): LocalDate {
        val candidate = YearMonth.of(forDate.year, endMonth).atEndOfMonth()
        return if (!forDate.isAfter(candidate)) candidate else {
            YearMonth.of(forDate.year + 1, endMonth).atEndOfMonth()
        }
    }

    fun yearStart(fiscalYearEnd: LocalDate): LocalDate = fiscalYearEnd.minusYears(1).plusDays(1)

    fun fiscalYearLabel(forDate: LocalDate, endMonth: Int): String {
        val fiscalYearEnd = yearEnd(forDate, endMonth)
        val start = yearStart(fiscalYearEnd)
        return if (start.year == fiscalYearEnd.year) "FY${fiscalYearEnd.year}"
        else "FY${start.year}/${fiscalYearEnd.year.toString().takeLast(2)}"
    }

    fun fiscalStartMonth(endMonth: Int): Int = (endMonth % 12) + 1

    fun daysUntilDayOfWeek(from: LocalDate, target: DayOfWeek): Int =
        (target.value - from.dayOfWeek.value + 7) % 7

    fun positionIn445(firstPeriodStart: LocalDate, periodStart: LocalDate): Int {
        val elapsed = ChronoUnit.DAYS.between(firstPeriodStart, periodStart)
        if (elapsed <= 0) return 0
        return when ((elapsed % 91L).toInt()) {
            in 0..27 -> 0
            in 28..55 -> 1
            in 56..90 -> 2
            else -> throw IllegalStateException("elapsed % 91 out of range: $elapsed")
        }
    }
}
