package me.ezra_home.retail_software_solution.organizations.business.unitconversion.api

import java.util.UUID

data class UnitConversionUpdateDto(
    val id: UUID,
    val numerator: Long,
    val denominator: Long
)
