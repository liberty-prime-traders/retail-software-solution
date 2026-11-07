package me.ezra_home.retail_software_solution.locations.business.reports.payments.filters

import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentVoidStatusFilter
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext

class PaymentVoidStatusFilterStrategy(
  private val statuses: Set<PaymentVoidStatusFilter>,
  private val voidIdColumn: String
) : FilterStrategy {

  // Both statuses (or none) keep every row, so only a single status adds a clause.
  override fun apply(context: QueryBuilderContext) {
    val onlyStatus = statuses.singleOrNull() ?: return
    context.whereClauses.add(
      when (onlyStatus) {
        PaymentVoidStatusFilter.ACTIVE -> "$voidIdColumn IS NULL"
        PaymentVoidStatusFilter.VOIDED -> "$voidIdColumn IS NOT NULL"
      }
    )
  }
}
