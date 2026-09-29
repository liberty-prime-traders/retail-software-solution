package me.ezra_home.retail_software_solution.locations.business.purchase.search.filters

import me.ezra_home.retail_software_solution.locations.business.purchase.search.PurchaseSearchAliases
import me.ezra_home.retail_software_solution.locations.business.purchase.search.PurchaseSearchParameterNames
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.math.BigDecimal


class AmountRangeFilterStrategy(
  private val minAmount: BigDecimal?,
  private val maxAmount: BigDecimal?
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    // COALESCE to 0 — a purchase with no lines has no row at all in the ordered-total join,
    // not a matched row with a zero total, so the raw column is NULL and would fail both bounds.
    minAmount?.let {
      context.whereClauses.add("COALESCE(${PurchaseSearchAliases.ORDERED_TOTAL}.ordered_total, 0) >= :${PurchaseSearchParameterNames.MIN_AMOUNT}")
      context.params[PurchaseSearchParameterNames.MIN_AMOUNT] = it
    }
    maxAmount?.let {
      context.whereClauses.add("COALESCE(${PurchaseSearchAliases.ORDERED_TOTAL}.ordered_total, 0) <= :${PurchaseSearchParameterNames.MAX_AMOUNT}")
      context.params[PurchaseSearchParameterNames.MAX_AMOUNT] = it
    }
  }
}
