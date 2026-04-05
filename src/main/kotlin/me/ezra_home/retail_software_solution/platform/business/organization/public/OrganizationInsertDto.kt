package me.ezra_home.retail_software_solution.platform.business.organization.public

import java.io.Serializable
import java.util.UUID

data class OrganizationInsertDto(
    val name: String,
    val subdomain: String,
    val passCode: UUID,
    val description: String? = null,
    val hidden: Boolean = false,
    val timezone: String? = null
) : Serializable
