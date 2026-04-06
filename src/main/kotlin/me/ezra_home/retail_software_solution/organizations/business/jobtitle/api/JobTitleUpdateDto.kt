package me.ezra_home.retail_software_solution.organizations.business.jobtitle.api

import me.ezra_home.retail_software_solution.organizations.business.jobtitle.JobTitleDto
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class JobTitleUpdateDto(
    val id: UUID? = null,
    val value: Optional<String>? = null,
) : Serializable {

    fun applyTo(existing: JobTitleDto): JobTitleDto = existing.copy(
        value = value?.orElse(existing.value) ?: existing.value
    )
}
