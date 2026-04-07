package me.ezra_home.retail_software_solution.platform.business.jurisdiction

import java.util.UUID

data class JurisdictionMappingContext(
    val typeNames: Map<UUID, String>,
    val jurisdictionNames: Map<UUID, String>,
    val taxTypesByJurisdiction: Map<UUID, List<UUID>>
)
