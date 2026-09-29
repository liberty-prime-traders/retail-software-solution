package me.ezra_home.retail_software_solution.organizations.business.accounting_config.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class FiscalPeriodCycle(override val code: String) : HasCode {
    WEEKLY("W"),
    MONTHLY("M"),
    QUARTERLY("Q"),
    SEMI_ANNUAL("SA"),
    ANNUAL("A"),
    FOUR_FOUR_FIVE("445")
}
