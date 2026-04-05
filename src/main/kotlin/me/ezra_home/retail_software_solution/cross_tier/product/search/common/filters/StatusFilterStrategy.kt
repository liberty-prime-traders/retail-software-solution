package me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames.STATUS_LIST
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases.ColumnNames.CrossTierProduct
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext

class StatusFilterStrategy(private val statusCodes: Set<String>) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (statusCodes.isNotEmpty()) {
      context.whereClauses.add("${CrossTierProduct.TABLE_ALIAS}.${CrossTierProduct.STATUS} = ANY(:$STATUS_LIST)")
      context.params[STATUS_LIST] = statusCodes.toTypedArray()
    }
  }
}
