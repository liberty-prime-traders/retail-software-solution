package me.ezra_home.retail_software_solution.platform.business.table_registry.dto

import java.util.Optional
import java.util.UUID

/**
 * DTO for updating TableRegistryEntity
 */
data class TableRegistryUpdateDto(
    val id: UUID?,
    val defaultPrefix: Optional<String>?,
    val displayName: Optional<String>?,
    val description: Optional<String>?,
    val userFacing: Optional<Boolean>?
)
