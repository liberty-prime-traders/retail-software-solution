package me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases.ColumnNames.CrossTierProduct
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames.PREVIOUS_NAME
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext

internal class NameFilterStrategy(private val previousName: String) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (previousName.isNotBlank()) {
      context.whereClauses.add("LOWER(${CrossTierProduct.TABLE_ALIAS}.${CrossTierProduct.PRODUCT_NAME}) > LOWER(:$PREVIOUS_NAME)")
      context.params[PREVIOUS_NAME] = previousName
    }
  }
}
