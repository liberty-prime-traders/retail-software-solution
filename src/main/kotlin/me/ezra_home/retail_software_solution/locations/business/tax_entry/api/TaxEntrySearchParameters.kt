package me.ezra_home.retail_software_solution.locations.business.tax_entry.api

import me.ezra_home.retail_software_solution.util.business.StringUtils
import java.math.BigDecimal
import java.util.UUID

data class TaxEntrySearchParameters(
    val fiscalPeriodIds: List<UUID> = emptyList(),
    val taxTypeIds: List<UUID> = emptyList(),
    val sourceTypes: Set<TaxSourceType> = emptySet(),
    val sourceReferenceNumbers: List<String> = emptyList(),
    val minTaxAmount: BigDecimal? = null,
    val maxTaxAmount: BigDecimal? = null
) {

    fun sanitized(): TaxEntrySearchParameters = copy(sourceReferenceNumbers = StringUtils.dropBlank(sourceReferenceNumbers))
}
