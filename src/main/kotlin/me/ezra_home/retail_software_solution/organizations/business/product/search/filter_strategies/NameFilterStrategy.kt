package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.Aliases
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames.PREVIOUS_NAME

class NameFilterStrategy(
  private val previousName: String
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (previousName.isNotBlank()) {
      val p = Aliases.ColumnNames.Product
      context.whereClauses.add("LOWER(${p.TABLE_ALIAS}.${p.NAME}) > LOWER(:$PREVIOUS_NAME)")
      context.params[PREVIOUS_NAME] = previousName
    }
  }
}
