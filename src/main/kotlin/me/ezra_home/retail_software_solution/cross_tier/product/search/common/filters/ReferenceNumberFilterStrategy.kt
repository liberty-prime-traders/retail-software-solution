package me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext

internal class ReferenceNumberFilterStrategy(private val referenceNumber: String?) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    referenceNumber?.takeIf { it.isNotBlank() }?.let { ref ->
      val p = Aliases.ColumnNames.CrossTierProduct
      context.whereClauses.add("${p.TABLE_ALIAS}.${p.REFERENCE_NUMBER} LIKE :${ParameterNames.REFERENCE_NUMBER}")
      context.params[ParameterNames.REFERENCE_NUMBER] = "$ref%"
    }
  }
}
