package me.ezra_home.retail_software_solution.locations.business.tax_entry.search

import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntrySearchParameters
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.queries.SearchGuards
import java.math.BigDecimal

object TaxEntrySearchValidator {

    private const val MAX_SOURCE_REFERENCE_NUMBERS = 20
    private const val MAX_FISCAL_PERIOD_IDS = 24
    private const val MAX_TAX_TYPE_IDS = 50
    private const val MIN_PAGE_SIZE = 1
    private const val MAX_PAGE_SIZE = 500

    fun guardValidParameters(taxEntrySearchParameters: TaxEntrySearchParameters) {
        guardFiscalPeriodSupplied(taxEntrySearchParameters)
        SearchGuards.guardMaxSize(taxEntrySearchParameters.fiscalPeriodIds.size, MAX_FISCAL_PERIOD_IDS, "fiscal period ids")
        SearchGuards.guardMaxSize(taxEntrySearchParameters.taxTypeIds.size, MAX_TAX_TYPE_IDS, "tax type ids")
        SearchGuards.guardMaxSize(
            taxEntrySearchParameters.sourceReferenceNumbers.size,
            MAX_SOURCE_REFERENCE_NUMBERS,
            "source reference numbers"
        )

        SearchGuards.guardRangeOrder(
            taxEntrySearchParameters.minTaxAmount,
            taxEntrySearchParameters.maxTaxAmount,
            "minTaxAmount must not be greater than maxTaxAmount"
        ) { min, max -> min > max }

        // Both bounds filter on ABS(tax_amount), so a negative bound is not a looser filter — it is
        // meaningless: min < 0 is a no-op (magnitude is never negative) and max < 0 empties the result.
        guardNonNegative(taxEntrySearchParameters.minTaxAmount, "minTaxAmount")
        guardNonNegative(taxEntrySearchParameters.maxTaxAmount, "maxTaxAmount")
    }

    private fun guardNonNegative(value: BigDecimal?, label: String) {
        if (value != null && value < BigDecimal.ZERO) {
            throw RtsGenericException("$label must not be negative")
        }
    }

    fun guardValidPageSize(requestedSize: Int) {
        SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
    }

    fun guardFiscalPeriodSupplied(taxEntrySearchParameters: TaxEntrySearchParameters) {
        if (taxEntrySearchParameters.fiscalPeriodIds.isEmpty()) {
            throw RtsGenericException("At least one fiscal period must be supplied to search tax entries")
        }
    }
}
