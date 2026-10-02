package me.ezra_home.retail_software_solution.locations.business.tax_entry.search

import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxDirection
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxSourceType
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class TaxEntrySearchRawRow(
    val id: UUID,
    val sourceReferenceNumber: String,
    val sourceType: TaxSourceType,
    val direction: TaxDirection,
    val taxTypeId: UUID,
    val fiscalPeriodId: UUID,
    val calculationMethod: CalculationMethod,
    val rate: BigDecimal,
    val taxInclusive: Boolean,
    val taxableAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val createdOn: OffsetDateTime
)

data class TaxEntrySourceTypeSummaryRawRow(
    val fiscalPeriodId: UUID,
    val taxTypeId: UUID,
    val sourceType: TaxSourceType,
    val entryCount: Long,
    val taxableAmount: BigDecimal,
    val taxAmount: BigDecimal
)
