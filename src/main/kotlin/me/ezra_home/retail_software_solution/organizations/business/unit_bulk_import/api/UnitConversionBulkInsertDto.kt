package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api

data class UnitConversionBulkInsertDto(
    val fromUnitCode: String?,
    val toUnitCode: String?,
    val numerator: Long?,
    val denominator: Long?
)
