package me.ezra_home.retail_software_solution.organizations.business.org_table_registry.dto

import java.util.UUID

data class OrgTableRegistryDto(
    var id: UUID? = null,
    var registryId: UUID? = null,
    var defaultPrefix: String? = null,
    var displayName: String? = null
)
