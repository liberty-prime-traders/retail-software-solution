package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api

data class BulkUnitImportRequestDto(
    val unitGroups: List<UnitGroupBulkInsertDto> = emptyList(),
    val unitConversions: List<UnitConversionBulkInsertDto> = emptyList()
)
