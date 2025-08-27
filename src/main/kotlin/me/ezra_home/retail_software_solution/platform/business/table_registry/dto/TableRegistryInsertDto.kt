package me.ezra_home.retail_software_solution.platform.business.table_registry.dto

import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import java.util.UUID

/**
 * DTO for creating TableRegistryEntity
 */
data class TableRegistryInsertDto(
    val tableName: String?,
    val defaultPrefix: String?,
    val minimumVersionId: UUID?,
    val schemaLevel: SchemaLevel?,
    val displayName: String?,
    val description: String?,
    val userFacing: Boolean?
)
