package me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api

import java.util.UUID

data class FiscalPeriodRenameDto(
    val id: UUID,
    val name: String
)
