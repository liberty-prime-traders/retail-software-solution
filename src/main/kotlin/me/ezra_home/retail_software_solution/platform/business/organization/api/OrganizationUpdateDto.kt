package me.ezra_home.retail_software_solution.platform.business.organization.api

import java.io.Serializable
import java.util.Optional

data class OrganizationUpdateDto(
    val name: Optional<String>? = null,
    val description: Optional<String>? = null,
    val hidden: Optional<Boolean>? = null,
    val timezone: Optional<String>? = null
) : Serializable {

    fun applyTo(existing: OrganizationDto): OrganizationDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        description = description?.orElse(null) ?: existing.description,
        hidden = hidden?.orElse(existing.hidden) ?: existing.hidden,
        timezone = timezone?.orElse(null) ?: existing.timezone
    )
}
