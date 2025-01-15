package me.ezra_home.retail_software_solution.business.title.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.TitleEntity}
 */
data class TitleUpdateDto (
    val id: UUID? = null,
    val value: Optional<String>? = null,
) : Serializable