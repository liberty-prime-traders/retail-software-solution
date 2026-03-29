package me.ezra_home.retail_software_solution.platform.business.tax_type.dto

import me.ezra_home.retail_software_solution.platform.business.tax_type.CalculationMethod
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxApplicationLevel
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxRecoveryType
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTrigger
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class TaxTypeResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val createdOn: OffsetDateTime?,
    val name: String,
    val description: String?,
    val calculationMethod: CalculationMethod,
    val taxRecoveryType: TaxRecoveryType,
    val taxApplicationLevel: TaxApplicationLevel,
    val taxTriggers: List<TaxTrigger>
) : Serializable
