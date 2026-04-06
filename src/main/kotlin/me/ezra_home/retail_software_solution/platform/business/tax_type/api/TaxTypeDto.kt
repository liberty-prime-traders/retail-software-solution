package me.ezra_home.retail_software_solution.platform.business.tax_type.api

import java.time.OffsetDateTime
import java.util.UUID

data class TaxTypeDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val name: String,
    val description: String? = null,
    val calculationMethod: CalculationMethod,
    val taxRecoveryType: TaxRecoveryType,
    val taxApplicationLevel: TaxApplicationLevel,
    val taxTriggers: List<TaxTrigger>
)
