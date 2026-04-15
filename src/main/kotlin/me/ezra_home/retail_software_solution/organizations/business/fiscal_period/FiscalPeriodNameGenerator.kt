package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.FiscalPeriodCycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

object FiscalPeriodNameGenerator {

    private val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    fun generate(start: LocalDate, cycle: FiscalPeriodCycle, periodNumberInYear: Int, fiscalYearEndMonth: Int): String {
        val fiscalStartMonth = (fiscalYearEndMonth % 12) + 1
        return when (cycle) {
            FiscalPeriodCycle.MONTHLY -> start.format(monthFormatter)
            FiscalPeriodCycle.QUARTERLY -> {
                val q = ((start.monthValue - fiscalStartMonth + 12) % 12) / 3 + 1
                "Q$q ${start.year}"
            }
            FiscalPeriodCycle.SEMI_ANNUAL -> {
                val h = ((start.monthValue - fiscalStartMonth + 12) % 12) / 6 + 1
                "H$h ${start.year}"
            }
            FiscalPeriodCycle.ANNUAL -> "FY ${start.year}"
            FiscalPeriodCycle.WEEKLY -> "W${String.format("%02d", start.get(WeekFields.ISO.weekOfWeekBasedYear()))} ${start.get(WeekFields.ISO.weekBasedYear())}"
            FiscalPeriodCycle.FOUR_FOUR_FIVE -> "P${String.format("%02d", periodNumberInYear)} FY${start.year}"
        }
    }
}
