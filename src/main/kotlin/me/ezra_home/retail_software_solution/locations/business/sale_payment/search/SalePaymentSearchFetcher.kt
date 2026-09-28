package me.ezra_home.retail_software_solution.locations.business.sale_payment.search

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSearchParameters
import org.springframework.stereotype.Component

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentSearchFetcher(
  private val salePaymentSearchExecutor: SalePaymentSearchExecutor
) {

  fun search(
    salePaymentSearchParameters: SalePaymentSearchParameters,
    cursor: SalePaymentSearchCursor?,
    requestedSize: Int
  ): List<SalePaymentSearchRawRow> {
    val predicate = SalePaymentSearchQueryBuilder.buildPredicate(salePaymentSearchParameters)
    val sqlQuery = SalePaymentSearchQueryBuilder.buildListQuery(predicate, cursor)
    return salePaymentSearchExecutor.execute(sqlQuery, requestedSize + 1, setTimeout = true)
      .map { SalePaymentSearchRowMapper.fromTuple(it) }
  }

  fun summarize(salePaymentSearchParameters: SalePaymentSearchParameters): List<SalePaymentMethodSummaryRawRow> {
    val predicate = SalePaymentSearchQueryBuilder.buildPredicate(salePaymentSearchParameters)
    val sqlQuery = SalePaymentSearchQueryBuilder.buildSummaryQuery(predicate)
    return salePaymentSearchExecutor.executeUnpaged(sqlQuery, setTimeout = true)
      .map { SalePaymentSearchRowMapper.summaryFromTuple(it) }
  }
}
