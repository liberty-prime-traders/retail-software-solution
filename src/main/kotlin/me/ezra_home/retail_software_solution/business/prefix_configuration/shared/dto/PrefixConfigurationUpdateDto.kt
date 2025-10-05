package me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto

import java.io.Serializable
import java.util.UUID

data class PrefixConfigurationUpdateDto(
    val prefixConfigurationId: UUID,
    val prefix: String? = null,
) : Serializable
