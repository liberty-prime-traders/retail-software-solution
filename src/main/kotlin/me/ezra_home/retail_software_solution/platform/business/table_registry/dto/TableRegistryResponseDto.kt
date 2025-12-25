package me.ezra_home.retail_software_solution.platform.business.table_registry.dto

import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import java.io.Serializable
import java.util.UUID

/**
 * DTO for responding with TableRegistryEntity
 */
data class TableRegistryResponseDto(
    val id: UUID?,
    val tableName: String?,
    val defaultPrefix: String?,
    val minimumVersion: String?,
    val schemaLevel: SchemaLevel?,
    val displayName: String?,
    val description: String?,
    val userFacing: Boolean?,
    val validated: Boolean?
): Serializable
