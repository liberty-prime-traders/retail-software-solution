package me.ezra_home.retail_software_solution.locations.business.purchase.search.strategies

import me.ezra_home.retail_software_solution.locations.business.purchase.search.PurchaseSearchParams.Columns
import me.ezra_home.retail_software_solution.locations.business.purchase.search.PurchaseSearchParams.Params
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.util.UUID

class CreatedByFilterStrategy(private val createdByIds: List<UUID>?) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (!createdByIds.isNullOrEmpty()) {
      context.whereClauses.add("p.${Columns.CREATED_BY_ID} = ANY(:${Params.CREATED_BY_IDS})")
      context.params[Params.CREATED_BY_IDS] = createdByIds.toTypedArray()
    }
  }
}
