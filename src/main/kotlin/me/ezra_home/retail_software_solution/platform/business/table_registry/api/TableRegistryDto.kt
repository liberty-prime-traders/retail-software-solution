package me.ezra_home.retail_software_solution.platform.business.table_registry.api

import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import java.util.UUID

data class TableRegistryDto(
    val id: UUID,
    val tableName: String,
    val defaultPrefix: String,
    val minimumVersionId: UUID,
    val schemaLevel: SchemaLevel,
    val displayName: String,
    val description: String,
    val userFacing: Boolean = false,
    val validated: Boolean = false
)
