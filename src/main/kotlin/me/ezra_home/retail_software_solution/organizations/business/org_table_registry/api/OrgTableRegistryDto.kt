package me.ezra_home.retail_software_solution.organizations.business.org_table_registry.api

import java.util.UUID

data class OrgTableRegistryDto(
    val id: UUID,
    val registryId: UUID,
    val defaultPrefix: String,
    val displayName: String
)
