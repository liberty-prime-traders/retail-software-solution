package me.ezra_home.retail_software_solution.organizations.business.unitconversion.api

import java.util.UUID

data class UnitConversionInsertDto(
    val fromUnitId: UUID,
    val toUnitId: UUID,
    val numerator: Long,
    val denominator: Long
)
