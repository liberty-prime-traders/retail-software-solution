package me.ezra_home.retail_software_solution.locations.business.tax_entry.api

import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import java.math.BigDecimal
import java.util.UUID

data class TaxEntryDto(
    val sourceReferenceNumber: String,
    val sourceType: TaxSourceType,
    val taxTypeId: UUID,
    val fiscalPeriodId: UUID,
    val calculationMethod: CalculationMethod,
    val rate: BigDecimal,
    val taxInclusive: Boolean,
    val taxableAmount: BigDecimal,
    val taxAmount: BigDecimal,
)
