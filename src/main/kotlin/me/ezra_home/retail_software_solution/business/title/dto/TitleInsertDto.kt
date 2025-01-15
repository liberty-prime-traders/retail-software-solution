package me.ezra_home.retail_software_solution.business.title.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.TitleEntity}
 */
data class TitleInsertDto(
    val value: String? = null,
) : Serializable