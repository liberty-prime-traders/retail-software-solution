package me.ezra_home.retail_software_solution.organizations.business.org_table_registry

import java.util.UUID

data class OrgTableRegistryDto(
    var id: UUID? = null,
    var registryId: UUID,
    var defaultPrefix: String,
    var displayName: String
)
