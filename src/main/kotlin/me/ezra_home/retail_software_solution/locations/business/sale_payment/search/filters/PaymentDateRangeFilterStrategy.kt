package me.ezra_home.retail_software_solution.locations.business.sale_payment.search.filters

import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchAliases
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchParameterNames
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.time.OffsetDateTime

class PaymentDateRangeFilterStrategy(
  private val paymentDateFrom: OffsetDateTime?,
  private val paymentDateBefore: OffsetDateTime?
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    paymentDateFrom?.let {
      context.whereClauses.add("${SalePaymentSearchAliases.SALE_PAYMENT}.payment_date >= :${SalePaymentSearchParameterNames.PAYMENT_DATE_FROM}")
      context.params[SalePaymentSearchParameterNames.PAYMENT_DATE_FROM] = it
    }
    paymentDateBefore?.let {
      context.whereClauses.add("${SalePaymentSearchAliases.SALE_PAYMENT}.payment_date < :${SalePaymentSearchParameterNames.PAYMENT_DATE_BEFORE}")
      context.params[SalePaymentSearchParameterNames.PAYMENT_DATE_BEFORE] = it
    }
  }
}
