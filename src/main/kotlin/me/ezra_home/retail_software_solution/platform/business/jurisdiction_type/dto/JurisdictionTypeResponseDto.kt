package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class JurisdictionTypeResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val createdOn: OffsetDateTime?,
    val name: String,
    val description: String?
) : Serializable
