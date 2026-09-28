package me.ezra_home.retail_software_solution.locations.business.sale_payment.search.filters

import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchAliases
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchParameterNames
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.math.BigDecimal

class AmountRangeFilterStrategy(
  private val minAmount: BigDecimal?,
  private val maxAmount: BigDecimal?
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    minAmount?.let {
      context.whereClauses.add("${SalePaymentSearchAliases.SALE_PAYMENT}.amount >= :${SalePaymentSearchParameterNames.MIN_AMOUNT}")
      context.params[SalePaymentSearchParameterNames.MIN_AMOUNT] = it
    }
    maxAmount?.let {
      context.whereClauses.add("${SalePaymentSearchAliases.SALE_PAYMENT}.amount <= :${SalePaymentSearchParameterNames.MAX_AMOUNT}")
      context.params[SalePaymentSearchParameterNames.MAX_AMOUNT] = it
    }
  }
}
