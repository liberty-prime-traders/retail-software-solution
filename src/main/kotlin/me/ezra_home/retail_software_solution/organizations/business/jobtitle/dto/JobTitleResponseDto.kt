package me.ezra_home.retail_software_solution.organizations.business.jobtitle.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.JobTitleEntity}
 */
data class JobTitleResponseDto (
    val id: UUID?,
    val value: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val referenceNumber: String?
) : Serializable
