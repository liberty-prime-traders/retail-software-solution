package me.ezra_home.retail_software_solution.organizations.business.unitconversion

import java.util.UUID

data class UnitConversionDto(
    val id: UUID,
    val fromUnitId: UUID,
    val toUnitId: UUID,
    val numerator: Long,
    val denominator: Long
)
