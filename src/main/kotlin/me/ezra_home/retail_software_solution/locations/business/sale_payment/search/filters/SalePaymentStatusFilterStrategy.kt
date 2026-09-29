package me.ezra_home.retail_software_solution.locations.business.sale_payment.search.filters

import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentStatusFilter
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchAliases
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext

class SalePaymentStatusFilterStrategy(private val statuses: Set<SalePaymentStatusFilter>) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    val onlyStatus = statuses.singleOrNull() ?: return
    val voidJoinColumn = "${SalePaymentSearchAliases.SALE_PAYMENT_VOID}.id"
    val clause = when (onlyStatus) {
      SalePaymentStatusFilter.ACTIVE -> "$voidJoinColumn IS NULL"
      SalePaymentStatusFilter.VOIDED -> "$voidJoinColumn IS NOT NULL"
    }
    context.whereClauses.add(clause)
  }
}
