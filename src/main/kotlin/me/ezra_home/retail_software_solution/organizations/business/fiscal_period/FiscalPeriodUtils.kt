package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import java.time.LocalDate
import java.time.YearMonth

object FiscalPeriodUtils {

    fun yearEnd(forDate: LocalDate, endMonth: Int, endDay: Int): LocalDate {
        val maxDay = getMaxDayForMonth(endMonth, endDay, forDate.year)
        val candidate = LocalDate.of(forDate.year, endMonth, maxDay)
        return if (!forDate.isAfter(candidate)) candidate else {
            val nextYear = forDate.year + 1
            LocalDate.of(nextYear, endMonth, getMaxDayForMonth(endMonth, endDay, nextYear))
        }
    }

    fun yearStart(fiscalYearEnd: LocalDate): LocalDate = fiscalYearEnd.minusYears(1).plusDays(1)

    fun fiscalYearLabel(forDate: LocalDate, endMonth: Int, endDay: Int): String {
        val fiscalYearEnd = yearEnd(forDate, endMonth, endDay)
        val start = yearStart(fiscalYearEnd)
        return if (start.year == fiscalYearEnd.year) "FY${fiscalYearEnd.year}"
        else "FY${start.year}/${fiscalYearEnd.year.toString().takeLast(2)}"
    }

    private fun getMaxDayForMonth(endMonth: Int, endDay: Int, year: Int): Int {
        if (endMonth == 2 && endDay >= 28) {
            return lengthOfMonthInDays(endMonth, year)
        }
        return minOf(endDay, lengthOfMonthInDays(endMonth, year))
    }

    private fun lengthOfMonthInDays(month: Int, year: Int): Int = YearMonth.of(year, month).lengthOfMonth()
}
