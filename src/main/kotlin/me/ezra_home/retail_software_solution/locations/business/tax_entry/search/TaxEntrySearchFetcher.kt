package me.ezra_home.retail_software_solution.locations.business.tax_entry.search

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntrySearchParameters
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Component

@Component
@TransactionalOnLocationSchema(readOnly = true)
class TaxEntrySearchFetcher(
    private val taxEntrySearchExecutor: TaxEntrySearchExecutor
) {

    fun search(
        taxEntrySearchParameters: TaxEntrySearchParameters,
        cursor: KeysetSearchCursor?,
        requestedSize: Int
    ): List<TaxEntrySearchRawRow> {
        val predicate = TaxEntrySearchQueryBuilder.buildPredicate(taxEntrySearchParameters)
        val sqlQuery = TaxEntrySearchQueryBuilder.buildListQuery(predicate, cursor)
        return taxEntrySearchExecutor.execute(sqlQuery, requestedSize + 1, setTimeout = true)
            .map { TaxEntrySearchRowMapper.fromTuple(it) }
    }

    fun summarize(taxEntrySearchParameters: TaxEntrySearchParameters): List<TaxEntrySourceTypeSummaryRawRow> {
        val predicate = TaxEntrySearchQueryBuilder.buildPredicate(taxEntrySearchParameters)
        val sqlQuery = TaxEntrySearchQueryBuilder.buildSummaryQuery(predicate)
        return taxEntrySearchExecutor.executeUnpaged(sqlQuery, setTimeout = true)
            .map { TaxEntrySearchRowMapper.summaryFromTuple(it) }
    }
}
