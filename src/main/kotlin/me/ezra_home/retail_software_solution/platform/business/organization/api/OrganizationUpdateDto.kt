package me.ezra_home.retail_software_solution.platform.business.organization.api

import java.io.Serializable
import java.util.Optional

data class OrganizationUpdateDto(
    val name: Optional<String>? = null,
    val description: Optional<String>? = null,
    val hidden: Optional<Boolean>? = null,
    val timezone: Optional<String>? = null
) : Serializable
