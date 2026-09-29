package me.ezra_home.retail_software_solution.platform.business.table_registry.api

import java.util.Optional
import java.util.UUID

data class TableRegistryUpdateDto(
    val id: UUID,
    val defaultPrefix: Optional<String>?,
    val displayName: Optional<String>?,
    val description: Optional<String>?,
    val userFacing: Optional<Boolean>?
) {

    fun applyTo(existing: TableRegistryDto): TableRegistryDto = existing.copy(
        defaultPrefix = defaultPrefix?.orElse(existing.defaultPrefix) ?: existing.defaultPrefix,
        displayName = displayName?.orElse(existing.displayName) ?: existing.displayName,
        description = description?.orElse(existing.description) ?: existing.description,
        userFacing = userFacing?.orElse(existing.userFacing) ?: existing.userFacing
    )
}
