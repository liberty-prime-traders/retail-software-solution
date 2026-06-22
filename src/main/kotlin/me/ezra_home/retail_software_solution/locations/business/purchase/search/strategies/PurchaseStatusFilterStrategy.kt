package me.ezra_home.retail_software_solution.locations.business.purchase.search.strategies

import me.ezra_home.retail_software_solution.locations.business.purchase.search.PurchaseSearchParams.Columns
import me.ezra_home.retail_software_solution.locations.business.purchase.search.PurchaseSearchParams.Params
import me.ezra_home.retail_software_solution.util.enums.PurchaseStatus
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext

class PurchaseStatusFilterStrategy(private val statuses: List<PurchaseStatus>?) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (!statuses.isNullOrEmpty()) {
      context.whereClauses.add("p.${Columns.STATUS} = ANY(:${Params.STATUSES})")
      context.params[Params.STATUSES] = statuses.map { it.code }.toTypedArray()
    }
  }
}
