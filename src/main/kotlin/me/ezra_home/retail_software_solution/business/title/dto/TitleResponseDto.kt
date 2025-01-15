package me.ezra_home.retail_software_solution.business.title.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.TitleEntity}
 */
data class TitleResponseDto (
    val id: UUID?,
    val value: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val usageCount: Long?
) : Serializable