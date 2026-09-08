package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api

import java.math.BigDecimal

data class UnitConversionBulkInsertDto(
    val fromUnitCode: String?,
    val toUnitCode: String?,
    val unitsOfBasePerUnit: BigDecimal?
)
