package me.ezra_home.retail_software_solution.platform.business.db_version.dto

import java.io.Serializable
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.platform.model.DbVersionEntity}
 */
data class DbVersionInsertDto(
    val versionNumber: String? = null,
    val prevVersionId: UUID? = null
) : Serializable
