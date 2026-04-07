package me.ezra_home.retail_software_solution.organizations.business.unitgroup.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class UnitGroupResponseDto(
    val id: UUID,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val name: String?,
    val description: String?,
    val referenceNumber: String?
) : Serializable
