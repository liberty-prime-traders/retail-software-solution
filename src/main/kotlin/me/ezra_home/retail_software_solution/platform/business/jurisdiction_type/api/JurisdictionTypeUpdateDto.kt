package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class JurisdictionTypeUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
) : Serializable {

    fun applyTo(existing: JurisdictionTypeDto): JurisdictionTypeDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        description = description?.orElse(null) ?: existing.description
    )
}
