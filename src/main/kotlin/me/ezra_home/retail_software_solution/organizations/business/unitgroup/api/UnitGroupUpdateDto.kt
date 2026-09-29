package me.ezra_home.retail_software_solution.organizations.business.unitgroup.api

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class UnitGroupUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
) : Serializable {

    fun applyTo(existing: UnitGroupDto): UnitGroupDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        description = description?.orElse(existing.description) ?: existing.description
    )
}
