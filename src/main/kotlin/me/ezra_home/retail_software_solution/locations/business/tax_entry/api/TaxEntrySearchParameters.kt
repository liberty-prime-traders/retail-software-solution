package me.ezra_home.retail_software_solution.locations.business.tax_entry.api

import java.math.BigDecimal
import java.util.UUID

data class TaxEntrySearchParameters(
    val fiscalPeriodIds: List<UUID> = emptyList(),
    val taxTypeIds: List<UUID> = emptyList(),
    val sourceTypes: Set<TaxSourceType> = emptySet(),
    val sourceReferenceNumbers: List<String> = emptyList(),
    val minTaxAmount: BigDecimal? = null,
    val maxTaxAmount: BigDecimal? = null
)
