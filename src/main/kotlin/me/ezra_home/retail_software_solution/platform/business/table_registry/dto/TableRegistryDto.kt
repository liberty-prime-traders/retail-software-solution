package me.ezra_home.retail_software_solution.platform.business.table_registry.dto

import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.model.HasId
import java.util.UUID

data class TableRegistryDto(
    override var id: UUID? = null,
    var tableName: String,
    var defaultPrefix: String,
    var minimumVersionId: UUID,
    var schemaLevel: SchemaLevel,
    var displayName: String,
    var description: String,
    var userFacing: Boolean = false,
    var validated: Boolean = false
) : HasId
