package me.ezra_home.retail_software_solution.platform.business.feature

import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import java.util.UUID

data class FeatureDto(
    val id: UUID,
    val feature: Feature,
    val description: String?
)
