package me.ezra_home.retail_software_solution.locations.business.jobtitle.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.JobTitleEntity}
 */
data class JobTitleUpdateDto (
    val id: UUID? = null,
    val value: Optional<String>? = null,
) : Serializable
