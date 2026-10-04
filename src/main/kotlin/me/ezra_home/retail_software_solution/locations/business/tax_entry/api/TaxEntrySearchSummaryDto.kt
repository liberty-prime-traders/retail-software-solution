package me.ezra_home.retail_software_solution.locations.business.tax_entry.api

import java.math.BigDecimal
import java.util.UUID

data class TaxTypePeriodSummaryDto(
    val fiscalPeriodId: UUID,
    val fiscalPeriodName: String,
    val taxTypeId: UUID,
    val taxTypeName: String,
    val jurisdictionName: String,
    val entryCount: Long,
    val grossTaxable: BigDecimal,
    val grossTax: BigDecimal,
    val reversalTax: BigDecimal,
    val netTax: BigDecimal
)

data class TaxEntrySearchSummaryResponseDto(
    val groups: List<TaxTypePeriodSummaryDto>,
    val grossTax: BigDecimal,
    val reversalTax: BigDecimal,
    val netTax: BigDecimal,
    val entryCount: Long
)
