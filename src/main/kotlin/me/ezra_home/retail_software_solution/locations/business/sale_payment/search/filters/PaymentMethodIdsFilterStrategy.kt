package me.ezra_home.retail_software_solution.locations.business.sale_payment.search.filters

import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchAliases
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchParameterNames
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.util.UUID

class PaymentMethodIdsFilterStrategy(private val paymentMethodIds: List<UUID>) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (paymentMethodIds.isNotEmpty()) {
      context.whereClauses.add("${SalePaymentSearchAliases.SALE_PAYMENT}.payment_method_id = ANY(:${SalePaymentSearchParameterNames.PAYMENT_METHOD_IDS})")
      context.params[SalePaymentSearchParameterNames.PAYMENT_METHOD_IDS] = paymentMethodIds.toTypedArray()
    }
  }
}
