package me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto

import java.util.UUID
import java.io.Serializable

data class PrefixConfigurationInsertDto(
    val tableRegistryId: UUID? = null,
    val prefix: String? = null
): Serializable