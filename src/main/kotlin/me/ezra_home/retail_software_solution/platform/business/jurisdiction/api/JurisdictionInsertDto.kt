package me.ezra_home.retail_software_solution.platform.business.jurisdiction.api

import java.io.Serializable
import java.util.UUID

data class JurisdictionInsertDto(
    val name: String,
    val jurisdictionTypeId: UUID,
    val parentJurisdictionId: UUID? = null,
    val taxTypesToAddOrReactivate: List<UUID>? = null
) : Serializable
