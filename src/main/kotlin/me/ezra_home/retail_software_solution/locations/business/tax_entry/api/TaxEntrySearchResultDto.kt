package me.ezra_home.retail_software_solution.locations.business.tax_entry.api

import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class TaxEntrySearchResultDto(
    val id: UUID,
    val sourceReferenceNumber: String,
    val sourceType: TaxSourceType,
    val direction: TaxDirection,
    val taxTypeId: UUID,
    val taxTypeName: String,
    val jurisdictionName: String,
    val fiscalPeriodId: UUID,
    val fiscalPeriodName: String,
    val calculationMethod: CalculationMethod,
    val rate: BigDecimal,
    val taxIsBilledToCustomerSeparately: Boolean,
    val taxIsIncludedInTaxableAmount: Boolean,
    val taxableAmount: BigDecimal,
    val resolvedTaxableBase: BigDecimal,
    val taxAmount: BigDecimal,
    val recordedOn: OffsetDateTime
)
