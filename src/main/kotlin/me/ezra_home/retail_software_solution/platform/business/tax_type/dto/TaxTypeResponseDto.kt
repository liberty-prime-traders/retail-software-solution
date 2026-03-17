package me.ezra_home.retail_software_solution.platform.business.tax_type.dto

import me.ezra_home.retail_software_solution.util.enums.CalculationMethod
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class TaxTypeResponseDto(
    val id: UUID?,
    val referenceNumber: String?,
    val createdOn: OffsetDateTime?,
    val name: String,
    val description: String?,
    val calculationMethod: CalculationMethod
) : Serializable
