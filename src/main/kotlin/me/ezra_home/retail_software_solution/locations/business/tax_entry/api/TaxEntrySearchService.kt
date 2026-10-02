package me.ezra_home.retail_software_solution.locations.business.tax_entry.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.tax_entry.search.TaxEntrySearchFetcher
import me.ezra_home.retail_software_solution.locations.business.tax_entry.search.TaxEntrySearchMapper
import me.ezra_home.retail_software_solution.locations.business.tax_entry.search.TaxEntrySearchValidator
import me.ezra_home.retail_software_solution.locations.business.tax_entry.search.TaxEntrySourceTypeSummaryRawRow
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class TaxEntrySearchService(
    private val taxEntrySearchFetcher: TaxEntrySearchFetcher,
    private val jurisdictionTaxTypeFetcher: JurisdictionTaxTypeFetcher,
    private val fiscalPeriodService: FiscalPeriodService
) {

    fun search(pageRequest: PageRequest<TaxEntrySearchParameters, String>): PageResponse<TaxEntrySearchResultDto, String> {
        TaxEntrySearchValidator.guardValidParameters(pageRequest.parameters)
        TaxEntrySearchValidator.guardValidPageSize(pageRequest.requestedSize)
        val cursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

        val rawRows = taxEntrySearchFetcher.search(pageRequest.parameters, cursor, pageRequest.requestedSize)
        val hasMore = rawRows.size > pageRequest.requestedSize
        val pageRows = if (hasMore) rawRows.take(pageRequest.requestedSize) else rawRows

        val taxTypeNamesById = resolveTaxTypeNames(pageRows.map { it.taxTypeId })
        val fiscalPeriodNamesById = resolveFiscalPeriodNames(pageRows.map { it.fiscalPeriodId })
        val contents = pageRows.map { TaxEntrySearchMapper.toRowDto(it, taxTypeNamesById, fiscalPeriodNamesById) }

        val currentCursor = pageRows.lastOrNull()
            ?.let { KeysetSearchCursor(it.createdOn, it.id).encode() }
            ?: pageRequest.previousCursor

        return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
    }

    fun summarize(taxEntrySearchParameters: TaxEntrySearchParameters): TaxEntrySearchSummaryResponseDto {
        TaxEntrySearchValidator.guardValidParameters(taxEntrySearchParameters)
        val rawRows = taxEntrySearchFetcher.summarize(taxEntrySearchParameters)

        val taxTypeNamesById = resolveTaxTypeNames(rawRows.map { it.taxTypeId })
        val fiscalPeriodNamesById = resolveFiscalPeriodNames(rawRows.map { it.fiscalPeriodId })

        val groups = rawRows
            .groupBy { it.fiscalPeriodId to it.taxTypeId }
            .map { (key, rowsForGroup) -> foldGroup(key.first, key.second, rowsForGroup, taxTypeNamesById, fiscalPeriodNamesById) }
            .sortedWith(compareByDescending<TaxTypePeriodSummaryDto> { it.fiscalPeriodName }.thenBy { it.taxTypeName })

        return TaxEntrySearchSummaryResponseDto(
            groups = groups,
            grossTax = groups.sumOf { it.grossTax },
            reversalTax = groups.sumOf { it.reversalTax },
            netTax = groups.sumOf { it.netTax },
            entryCount = groups.sumOf { it.entryCount }
        )
    }

    private fun foldGroup(
        fiscalPeriodId: UUID,
        taxTypeId: UUID,
        rowsForGroup: List<TaxEntrySourceTypeSummaryRawRow>,
        taxTypeNamesById: Map<UUID, String>,
        fiscalPeriodNamesById: Map<UUID, String>
    ): TaxTypePeriodSummaryDto {
        val reversalRows = rowsForGroup.filter { it.sourceType == TaxSourceType.SALE_VOID }
        val grossRows = rowsForGroup.filter { it.sourceType != TaxSourceType.SALE_VOID }

        val grossTax = grossRows.sumOf { it.taxAmount }
        val reversalTax = reversalRows.sumOf { it.taxAmount }

        return TaxTypePeriodSummaryDto(
            fiscalPeriodId = fiscalPeriodId,
            fiscalPeriodName = fiscalPeriodNamesById[fiscalPeriodId] ?: fiscalPeriodId.toString(),
            taxTypeId = taxTypeId,
            taxTypeName = taxTypeNamesById[taxTypeId] ?: taxTypeId.toString(),
            entryCount = rowsForGroup.sumOf { it.entryCount },
            grossTaxable = grossRows.sumOf { it.taxableAmount },
            grossTax = grossTax,
            reversalTax = reversalTax,
            netTax = grossTax + reversalTax
        )
    }

    private fun resolveTaxTypeNames(ids: Collection<UUID>): Map<UUID, String> {
        if (ids.isEmpty()) return emptyMap()
        val idSet = ids.toSet()
        return jurisdictionTaxTypeFetcher.buildIndex()
            .filterKeys { it in idSet }
            .mapValues { it.value.label }
    }

    private fun resolveFiscalPeriodNames(ids: Collection<UUID>): Map<UUID, String> {
        if (ids.isEmpty()) return emptyMap()
        val idSet = ids.toSet()
        return fiscalPeriodService.getAll()
            .filter { it.id in idSet }
            .associate { it.id to it.name }
    }
}
