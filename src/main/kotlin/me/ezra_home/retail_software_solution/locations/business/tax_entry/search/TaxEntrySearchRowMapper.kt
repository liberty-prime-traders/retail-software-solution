package me.ezra_home.retail_software_solution.locations.business.tax_entry.search

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxDirection
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxSourceType
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.util.business.mappers.DateQualifier
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object TaxEntrySearchRowMapper {

    fun fromTuple(tuple: Tuple): TaxEntrySearchRawRow = TaxEntrySearchRawRow(
        id = tuple.get("id", UUID::class.java),
        sourceReferenceNumber = tuple.get("source_reference_number", String::class.java),
        sourceType = TaxSourceType.entries.first { it.code == tuple.get("source_type", String::class.java) },
        direction = TaxDirection.entries.first { it.code == tuple.get("direction", String::class.java) },
        taxTypeId = tuple.get("tax_type_id", UUID::class.java),
        fiscalPeriodId = tuple.get("fiscal_period_id", UUID::class.java),
        calculationMethod = CalculationMethod.entries.first { it.code == tuple.get("calculation_method", String::class.java) },
        rate = tuple.get("rate", BigDecimal::class.java),
        taxInclusive = tuple.get("tax_inclusive", Boolean::class.java),
        taxableAmount = tuple.get("taxable_amount", BigDecimal::class.java),
        taxAmount = tuple.get("tax_amount", BigDecimal::class.java),
        createdOn = requireNotNull(DateQualifier.toOffsetDateTime(tuple.get("created_on", Instant::class.java)))
    )

    fun summaryFromTuple(tuple: Tuple): TaxEntrySourceTypeSummaryRawRow = TaxEntrySourceTypeSummaryRawRow(
        fiscalPeriodId = tuple.get("fiscal_period_id", UUID::class.java),
        taxTypeId = tuple.get("tax_type_id", UUID::class.java),
        sourceType = TaxSourceType.entries.first { it.code == tuple.get("source_type", String::class.java) },
        entryCount = tuple.get("entry_count", Number::class.java).toLong(),
        taxableAmount = tuple.get("taxable_amount", BigDecimal::class.java),
        taxAmount = tuple.get("tax_amount", BigDecimal::class.java)
    )
}
