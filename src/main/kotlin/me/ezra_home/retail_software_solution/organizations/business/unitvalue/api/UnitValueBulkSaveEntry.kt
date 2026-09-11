package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

import java.util.UUID

data class UnitValueBulkSaveEntry(
    val id: UUID,
    val name: String,
    val code: String,
    val description: String?,
    val unitGroupId: UUID,
    val baseUnit: UUID?,
    val unitsOfBasePerUnit: Long?
)
