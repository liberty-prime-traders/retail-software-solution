package me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases.ColumnNames.CrossTierProduct
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames.CATEGORY_IDS
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.util.UUID

class CategoryFilterStrategy(
  private val categoryIds: Set<UUID>,
  private val tableAlias: String
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (categoryIds.isNotEmpty()) {
      context.whereClauses.add("$tableAlias.${CrossTierProduct.CATEGORY_ID} = ANY(:$CATEGORY_IDS)")
      context.params[CATEGORY_IDS] = categoryIds.toTypedArray()
    }
  }
}
