package me.ezra_home.retail_software_solution.platform.business.feature.api

import me.ezra_home.retail_software_solution.platform.business.feature.FeatureDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import java.util.Optional


data class FeatureUpdateDto(
    val feature: Feature,
    val description: Optional<String>? = null
) {
    fun applyTo(existing: FeatureDto): FeatureDto {
        return existing.copy(
            description = StringUtils.useIfProvided(description, existing.description)
        )
    }
}
