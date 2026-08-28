package me.ezra_home.retail_software_solution.organizations.business.unitconversion.api

import java.math.BigDecimal
import java.util.UUID

data class UnitConversionDto(
    val id: UUID,
    val fromUnitId: UUID,
    val toUnitId: UUID,
    val factor: BigDecimal
)
