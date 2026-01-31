package me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames.PREVIOUS_NAME
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext

class NameFilterStrategy(
  private val previousName: String,
  private val tableAlias: String = Aliases.ColumnNames.Product.TABLE_ALIAS
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (previousName.isNotBlank()) {
      context.whereClauses.add("LOWER($tableAlias.name) > LOWER(:$PREVIOUS_NAME)")
      context.params[PREVIOUS_NAME] = previousName
    }
  }
}
