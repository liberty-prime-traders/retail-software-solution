package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api

data class UnitValueBulkInsertDto(
    val name: String?,
    val code: String?,
    val description: String?,
    val baseUnitCode: String?,
    val unitsOfBasePerUnit: Double?
)
