package me.ezra_home.retail_software_solution.organizations.business.jobtitle.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class JobTitleResponseDto(
    val id: UUID,
    val value: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val referenceNumber: String?
) : Serializable
