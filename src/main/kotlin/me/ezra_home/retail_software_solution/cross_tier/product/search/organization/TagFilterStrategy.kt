package me.ezra_home.retail_software_solution.cross_tier.product.search.organization

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.util.UUID

class TagFilterStrategy(
  private val tagIds: Set<UUID>
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (tagIds.isNotEmpty()) {
      val pt = Aliases.ColumnNames.ProductTag

      context.whereClauses.add("${pt.TABLE_ALIAS}.${pt.TAG_ID} = ANY(:${ParameterNames.TAG_IDS})")
      context.whereClauses.add("${pt.TABLE_ALIAS}.${pt.END_ON} IS NULL")

      context.havingClauses.add("COUNT(DISTINCT ${pt.TABLE_ALIAS}.${pt.TAG_ID}) = :${ParameterNames.TAG_IDS_COUNT}")

      context.params[ParameterNames.TAG_IDS] = tagIds.toTypedArray()
      context.params[ParameterNames.TAG_IDS_COUNT] = tagIds.size
    }
  }
}
