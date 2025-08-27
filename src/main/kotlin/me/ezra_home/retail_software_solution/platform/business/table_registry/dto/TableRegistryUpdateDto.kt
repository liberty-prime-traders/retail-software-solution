package me.ezra_home.retail_software_solution.platform.business.table_registry.dto

import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import java.util.Optional
import java.util.UUID

/**
 * DTO for updating TableRegistryEntity
 */
data class TableRegistryUpdateDto(
    val id: UUID?,
    val tableName: Optional<String>?,
    val defaultPrefix: Optional<String>?,
    val minimumVersionId: Optional<UUID>?,
    val schemaLevel: Optional<SchemaLevel>?,
    val displayName: Optional<String>?,
    val description: Optional<String>?,
    val userFacing: Optional<Boolean>?
)
