package me.ezra_home.retail_software_solution.platform.business.tax_type.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class TaxTypeUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
) : Serializable
