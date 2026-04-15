package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import java.time.LocalDate
import java.time.YearMonth

class MonthlyPeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, config: OrgAccountingConfigDto): PeriodRange {
        val month = YearMonth.from(lastPeriodEnd).plusMonths(1)
        return PeriodRange(month.atDay(1), month.atEndOfMonth())
    }

    override fun nextCleanStart(from: LocalDate, config: OrgAccountingConfigDto): LocalDate {
        if (from.dayOfMonth == 1) return from
        return from.withDayOfMonth(1).plusMonths(1)
    }
}
