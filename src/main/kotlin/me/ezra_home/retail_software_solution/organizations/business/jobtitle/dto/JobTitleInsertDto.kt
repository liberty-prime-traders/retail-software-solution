package me.ezra_home.retail_software_solution.organizations.business.jobtitle.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.JobTitleEntity}
 */
data class JobTitleInsertDto(
    val value: String? = null,
) : Serializable
