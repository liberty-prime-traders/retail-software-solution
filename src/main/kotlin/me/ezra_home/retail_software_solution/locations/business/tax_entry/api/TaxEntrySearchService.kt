package me.ezra_home.retail_software_solution.locations.business.tax_entry.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.tax_entry.search.TaxEntrySearchFetcher
import me.ezra_home.retail_software_solution.locations.business.tax_entry.search.TaxEntrySearchMapper
import me.ezra_home.retail_software_solution.locations.business.tax_entry.search.TaxEntrySearchValidator
import me.ezra_home.retail_software_solution.locations.business.tax_entry.search.TaxEntrySourceTypeSummaryRawRow
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class TaxEntrySearchService(
    private val taxEntrySearchFetcher: TaxEntrySearchFetcher,
    private val fiscalPeriodService: FiscalPeriodService
) {

    fun search(pageRequest: PageRequest<TaxEntrySearchParameters, String>): PageResponse<TaxEntrySearchResultDto, String> {
        val taxEntrySearchParameters = pageRequest.parameters.sanitized()
        TaxEntrySearchValidator.guardValidParameters(taxEntrySearchParameters)
        TaxEntrySearchValidator.guardValidPageSize(pageRequest.requestedSize)
        val cursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

        val rawRows = taxEntrySearchFetcher.search(taxEntrySearchParameters, cursor, pageRequest.requestedSize)
        val hasMore = rawRows.size > pageRequest.requestedSize
        val pageRows = if (hasMore) rawRows.take(pageRequest.requestedSize) else rawRows

        val fiscalPeriodNamesById = resolveFiscalPeriodNames(pageRows.map { it.fiscalPeriodId })
        val contents = pageRows.map { TaxEntrySearchMapper.toRowDto(it, fiscalPeriodNamesById) }

        val currentCursor = pageRows.lastOrNull()
            ?.let { KeysetSearchCursor(it.createdOn, it.id).encode() }
            ?: pageRequest.previousCursor

        return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
    }

    fun summarize(taxEntrySearchParameters: TaxEntrySearchParameters): TaxEntrySearchSummaryResponseDto {
        val sanitizedTaxEntrySearchParameters = taxEntrySearchParameters.sanitized()
        TaxEntrySearchValidator.guardValidParameters(sanitizedTaxEntrySearchParameters)
        val rawRows = taxEntrySearchFetcher.summarize(sanitizedTaxEntrySearchParameters)

        val fiscalPeriodNamesById = resolveFiscalPeriodNames(rawRows.map { it.fiscalPeriodId })

        // Grouped by the name each row snapshotted at creation, not a live lookup by taxTypeId --
        // if a tax type or jurisdiction was renamed between two periods, that shows up as two
        // separate fragments here rather than being silently merged under today's name.
        val groups = rawRows
            .groupBy { TaxTypeFragmentKey(it.fiscalPeriodId, it.taxTypeId, it.taxTypeName, it.jurisdictionName) }
            .map { (key, rowsForGroup) -> foldGroup(key, rowsForGroup, fiscalPeriodNamesById) }
            .sortedWith(
                compareByDescending<TaxTypePeriodSummaryDto> { it.fiscalPeriodName }
                    .thenBy { it.taxTypeName }
                    .thenBy { it.jurisdictionName }
            )

        return TaxEntrySearchSummaryResponseDto(
            groups = groups,
            grossTax = groups.sumOf { it.grossTax },
            reversalTax = groups.sumOf { it.reversalTax },
            netTax = groups.sumOf { it.netTax },
            entryCount = groups.sumOf { it.entryCount }
        )
    }

    private data class TaxTypeFragmentKey(
        val fiscalPeriodId: UUID,
        val taxTypeId: UUID,
        val taxTypeName: String,
        val jurisdictionName: String
    )

    private fun foldGroup(
        key: TaxTypeFragmentKey,
        rowsForGroup: List<TaxEntrySourceTypeSummaryRawRow>,
        fiscalPeriodNamesById: Map<UUID, String>
    ): TaxTypePeriodSummaryDto {
        val reversalRows = rowsForGroup.filter { it.sourceType == TaxSourceType.SALE_VOID }
        val grossRows = rowsForGroup.filter { it.sourceType != TaxSourceType.SALE_VOID }

        val grossTax = grossRows.sumOf { it.taxAmount }
        val reversalTax = reversalRows.sumOf { it.taxAmount }

        return TaxTypePeriodSummaryDto(
            fiscalPeriodId = key.fiscalPeriodId,
            fiscalPeriodName = fiscalPeriodNamesById[key.fiscalPeriodId] ?: key.fiscalPeriodId.toString(),
            taxTypeId = key.taxTypeId,
            taxTypeName = key.taxTypeName,
            jurisdictionName = key.jurisdictionName,
            entryCount = rowsForGroup.sumOf { it.entryCount },
            grossTaxable = grossRows.sumOf { it.taxableAmount },
            grossTax = grossTax,
            reversalTax = reversalTax,
            netTax = grossTax + reversalTax
        )
    }

    private fun resolveFiscalPeriodNames(ids: Collection<UUID>): Map<UUID, String> {
        if (ids.isEmpty()) return emptyMap()
        val idSet = ids.toSet()
        return fiscalPeriodService.getAll()
            .filter { it.id in idSet }
            .associate { it.id to it.name }
    }
}
