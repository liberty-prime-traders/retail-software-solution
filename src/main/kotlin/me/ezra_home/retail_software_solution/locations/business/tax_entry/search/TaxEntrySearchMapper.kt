package me.ezra_home.retail_software_solution.locations.business.tax_entry.search

import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntrySearchResultDto
import java.util.UUID

object TaxEntrySearchMapper {

    fun toRowDto(
        row: TaxEntrySearchRawRow,
        taxTypeNamesById: Map<UUID, String>,
        fiscalPeriodNamesById: Map<UUID, String>
    ): TaxEntrySearchResultDto = TaxEntrySearchResultDto(
        id = row.id,
        sourceReferenceNumber = row.sourceReferenceNumber,
        sourceType = row.sourceType,
        direction = row.direction,
        taxTypeId = row.taxTypeId,
        taxTypeName = taxTypeNamesById.getValue(row.taxTypeId),
        fiscalPeriodId = row.fiscalPeriodId,
        fiscalPeriodName = fiscalPeriodNamesById.getValue(row.fiscalPeriodId),
        calculationMethod = row.calculationMethod,
        rate = row.rate,
        taxInclusive = row.taxInclusive,
        taxableAmount = row.taxableAmount,
        taxAmount = row.taxAmount,
        recordedOn = row.createdOn
    )
}
