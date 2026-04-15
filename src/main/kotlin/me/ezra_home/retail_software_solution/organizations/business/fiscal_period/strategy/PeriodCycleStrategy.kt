package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto
import java.time.LocalDate

interface PeriodCycleStrategy {

    fun nextPeriod(lastPeriodEnd: LocalDate, config: OrgAccountingConfigDto): PeriodRange

    fun nextCleanStart(from: LocalDate, config: OrgAccountingConfigDto): LocalDate
}
