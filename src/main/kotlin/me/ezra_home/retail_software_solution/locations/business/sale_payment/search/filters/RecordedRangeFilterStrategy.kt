package me.ezra_home.retail_software_solution.locations.business.sale_payment.search.filters

import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchAliases
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchParameterNames
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.time.OffsetDateTime

class RecordedRangeFilterStrategy(
  private val recordedFrom: OffsetDateTime?,
  private val recordedBefore: OffsetDateTime?
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    recordedFrom?.let {
      context.whereClauses.add("${SalePaymentSearchAliases.SALE_PAYMENT}.created_on >= :${SalePaymentSearchParameterNames.RECORDED_FROM}")
      context.params[SalePaymentSearchParameterNames.RECORDED_FROM] = it
    }
    recordedBefore?.let {
      context.whereClauses.add("${SalePaymentSearchAliases.SALE_PAYMENT}.created_on < :${SalePaymentSearchParameterNames.RECORDED_BEFORE}")
      context.params[SalePaymentSearchParameterNames.RECORDED_BEFORE] = it
    }
  }
}
