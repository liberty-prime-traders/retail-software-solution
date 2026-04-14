package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.strategy

import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigDto

data class PeriodGenerationContext(
    val config: OrgAccountingConfigDto,
    val existingPeriodsInCurrentYear: Int
)
