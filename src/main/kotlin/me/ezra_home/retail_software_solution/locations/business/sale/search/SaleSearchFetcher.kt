package me.ezra_home.retail_software_solution.locations.business.sale.search

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSearchParameters
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Component

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SaleSearchFetcher(
  private val saleSearchExecutor: SaleSearchExecutor
) {

  fun search(
    saleSearchParameters: SaleSearchParameters,
    cursor: KeysetSearchCursor?,
    requestedSize: Int
  ): List<SaleSearchRawRow> {
    val predicate = SaleSearchQueryBuilder.buildPredicate(saleSearchParameters)
    val sqlQuery = SaleSearchQueryBuilder.buildListQuery(predicate, cursor)
    return saleSearchExecutor.execute(sqlQuery, requestedSize + 1, setTimeout = true)
      .map { SaleSearchRowMapper.fromTuple(it) }
  }

  fun summarize(saleSearchParameters: SaleSearchParameters): List<SaleStatusSummaryRawRow> {
    val predicate = SaleSearchQueryBuilder.buildPredicate(saleSearchParameters)
    val sqlQuery = SaleSearchQueryBuilder.buildSummaryQuery(predicate)
    return saleSearchExecutor.executeUnpaged(sqlQuery, setTimeout = true)
      .map { SaleSearchRowMapper.summaryFromTuple(it) }
  }
}
