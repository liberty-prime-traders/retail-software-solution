package me.ezra_home.retail_software_solution.organizations.business.org_table_registry.api

import java.util.Optional
import java.util.UUID

data class OrgTableRegistryUpdateDto(
    val id: UUID,
    val defaultPrefix: Optional<String>?,
    val displayName: Optional<String>?
)
