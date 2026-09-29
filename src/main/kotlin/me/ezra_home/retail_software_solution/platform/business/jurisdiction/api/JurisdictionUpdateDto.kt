package me.ezra_home.retail_software_solution.platform.business.jurisdiction.api

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class JurisdictionUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val jurisdictionTypeId: Optional<UUID>? = null,
    val parentJurisdictionId: Optional<UUID>? = null,
    val taxTypesToAddOrReactivate: List<UUID>? = null,
    val taxTypesToDiscontinue: List<UUID>? = null
) : Serializable {

    fun applyTo(existing: JurisdictionDto): JurisdictionDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        jurisdictionTypeId = jurisdictionTypeId?.orElse(existing.jurisdictionTypeId) ?: existing.jurisdictionTypeId,
        parentJurisdictionId = if (parentJurisdictionId != null) parentJurisdictionId.orElse(null) else existing.parentJurisdictionId
    )
}
