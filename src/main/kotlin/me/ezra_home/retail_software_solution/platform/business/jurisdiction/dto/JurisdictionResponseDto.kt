package me.ezra_home.retail_software_solution.platform.business.jurisdiction.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class JurisdictionResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val createdOn: OffsetDateTime?,
    val name: String,
    val jurisdictionTypeId: UUID,
    val jurisdictionType: String?,
    val parentJurisdictionId: UUID?,
    val parentJurisdiction: String?,
    val taxTypes: List<UUID>
) : Serializable
