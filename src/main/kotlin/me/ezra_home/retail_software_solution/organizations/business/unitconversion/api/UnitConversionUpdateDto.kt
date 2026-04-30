package me.ezra_home.retail_software_solution.organizations.business.unitconversion.api

import java.math.BigDecimal
import java.util.UUID

data class UnitConversionUpdateDto(
    val id: UUID,
    val factor: BigDecimal
)
