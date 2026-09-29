package me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases.ColumnNames.CrossTierProduct
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames.EXCLUDE_IDS
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.util.UUID

class ExcludeIdsFilterStrategy(
  private val excludeIds: Set<UUID>,
  private val tableAlias: String
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (excludeIds.isNotEmpty()) {
      context.whereClauses.add("$tableAlias.${CrossTierProduct.ID} != ALL(:$EXCLUDE_IDS)")
      context.params[EXCLUDE_IDS] = excludeIds.toTypedArray()
    }
  }
}
