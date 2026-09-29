package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api

data class UnitGroupBulkInsertDto(
    val name: String?,
    val description: String?,
    val unitValues: List<UnitValueBulkInsertDto> = emptyList()
)
