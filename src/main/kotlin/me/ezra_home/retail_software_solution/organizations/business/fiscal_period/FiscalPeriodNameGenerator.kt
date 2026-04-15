package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.FiscalPeriodCycle
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object FiscalPeriodNameGenerator {

    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private const val TWO_DIGIT = "%02d"

    fun stubName(start: LocalDate): String = "Stub ${start.format(monthYearFormatter)}"

    fun generate(start: LocalDate, config: OrgAccountingConfigDto): String {
        val fiscalStartMonth = FiscalPeriodUtils.fiscalStartMonth(config.fiscalYearEndMonth)
        val fyStart = FiscalPeriodUtils.yearStart(FiscalPeriodUtils.yearEnd(start, config.fiscalYearEndMonth))
        return when (config.fiscalPeriodCycle) {
            FiscalPeriodCycle.MONTHLY -> start.format(monthYearFormatter)
            FiscalPeriodCycle.QUARTERLY -> {
                val monthOffset = (start.monthValue - fiscalStartMonth + 12) % 12
                "Q${monthOffset / 3 + 1} ${fyStart.year}"
            }
            FiscalPeriodCycle.SEMI_ANNUAL -> {
                val monthOffset = (start.monthValue - fiscalStartMonth + 12) % 12
                "H${monthOffset / 6 + 1} ${fyStart.year}"
            }
            FiscalPeriodCycle.ANNUAL -> "FY ${fyStart.year}"
            FiscalPeriodCycle.WEEKLY -> {
                val daysToFirstWeek = FiscalPeriodUtils.daysUntilDayOfWeek(fyStart, config.periodWeekStartDay)
                val firstWeekStart = fyStart.plusDays(daysToFirstWeek.toLong())
                val weekNumber = (ChronoUnit.DAYS.between(firstWeekStart, start) / 7 + 1).toInt()
                "W${String.format(TWO_DIGIT, weekNumber)} ${fyStart.year}"
            }
            FiscalPeriodCycle.FOUR_FOUR_FIVE -> {
                val firstPeriodStart = fyStart.plusDays(FiscalPeriodUtils.daysUntilDayOfWeek(fyStart, config.periodWeekStartDay).toLong())
                val elapsed = ChronoUnit.DAYS.between(firstPeriodStart, start)
                val periodNumber = if (elapsed <= 0) 1 else {
                    val posInGroup = FiscalPeriodUtils.positionIn445(firstPeriodStart, start)
                    (elapsed / 91L).toInt() * 3 + posInGroup + 1
                }
                "P${String.format(TWO_DIGIT, periodNumber)} FY${fyStart.year}"
            }
        }
    }
}
