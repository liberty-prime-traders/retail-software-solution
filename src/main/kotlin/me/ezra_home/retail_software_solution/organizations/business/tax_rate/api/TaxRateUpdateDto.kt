package me.ezra_home.retail_software_solution.organizations.business.tax_rate.api

import java.io.Serializable
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

data class TaxRateUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val endDate: Optional<LocalDate>? = null
) : Serializable
