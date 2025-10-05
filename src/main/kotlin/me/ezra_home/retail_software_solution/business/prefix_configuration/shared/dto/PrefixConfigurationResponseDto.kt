package me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class PrefixConfigurationResponseDto(
    val id: UUID?,
    val tableRegistryId: UUID?,
    val prefix: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val updatedOn: OffsetDateTime?
) : Serializable
