package me.ezra_home.retail_software_solution.locations.business.reports.payments

import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentSearchParameters
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Component

@Component
@TransactionalOnLocationSchema(readOnly = true)
class PaymentSearchFetcher(
  private val paymentSearchExecutor: PaymentSearchExecutor
) {

  // Returns one row beyond requestedSize so the caller can tell whether another page exists.
  fun search(
    paymentSources: List<PaymentSource>,
    paymentSearchParameters: PaymentSearchParameters,
    keysetSearchCursor: KeysetSearchCursor?,
    requestedSize: Int
  ): List<PaymentRow> {
    if (paymentSources.isEmpty()) return emptyList()
    val sqlQuery = PaymentSearchQueryBuilder.buildListQuery(paymentSources, paymentSearchParameters, keysetSearchCursor)
    return paymentSearchExecutor.execute(sqlQuery, requestedSize + 1, setTimeout = true)
      .map { PaymentSearchRowMapper.toPaymentRow(it) }
  }

  fun summarize(
    paymentSource: PaymentSource,
    paymentSearchParameters: PaymentSearchParameters
  ): List<PaymentMethodTotals> {
    val sqlQuery = PaymentSearchQueryBuilder.buildSummaryQuery(paymentSource, paymentSearchParameters)
    return paymentSearchExecutor.executeUnpaged(sqlQuery, setTimeout = true)
      .map { PaymentSearchRowMapper.toPaymentMethodTotals(it) }
  }
}
