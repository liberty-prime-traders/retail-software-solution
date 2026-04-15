package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import java.time.LocalDate
import java.time.YearMonth

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
}
