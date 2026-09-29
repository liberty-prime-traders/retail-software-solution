package me.ezra_home.retail_software_solution.organizations.business.feature.api

import me.ezra_home.retail_software_solution.organizations.business.feature.OrganizationFeatureStatus
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationFeatureResponseDto(
    val id: UUID?,
    val feature: Feature,
    val status: OrganizationFeatureStatus,
    val enabledOn: OffsetDateTime?,
    val enabledBy: String?,
    val disabledOn: OffsetDateTime?,
    val disabledBy: String?
) : Serializable
