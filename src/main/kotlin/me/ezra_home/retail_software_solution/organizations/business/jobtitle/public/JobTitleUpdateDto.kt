package me.ezra_home.retail_software_solution.organizations.business.jobtitle.public

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class JobTitleUpdateDto(
    val id: UUID? = null,
    val value: Optional<String>? = null,
) : Serializable
