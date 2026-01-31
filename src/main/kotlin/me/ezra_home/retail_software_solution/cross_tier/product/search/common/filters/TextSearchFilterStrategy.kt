package me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames.NAME_OR_DESCRIPTION
import me.ezra_home.retail_software_solution.util.queries.SearchStrategy
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext

class TextSearchFilterStrategy(
  private val searchText: String?,
  private val searchStrategy: SearchStrategy,
  private val includeDescription: Boolean = false
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    searchText?.takeIf { it.isNotBlank() }?.let { text ->
      if (searchStrategy == SearchStrategy.NONE) return

      val p = Aliases.ColumnNames.Product

      when (searchStrategy) {
        SearchStrategy.FULLTEXT -> {
          context.whereClauses.add(
            "${p.TABLE_ALIAS}.${p.SEARCH_VECTOR} @@ plainto_tsquery('english', :$NAME_OR_DESCRIPTION)"
          )
          context.params[NAME_OR_DESCRIPTION] = text
        }

        SearchStrategy.TRIGRAM -> {
          val conditions = buildString {
            append("LOWER(${p.TABLE_ALIAS}.${p.NAME}) % LOWER(:$NAME_OR_DESCRIPTION) ")
            append("OR LOWER(COALESCE(${p.TABLE_ALIAS}.${p.PRODUCT_GROUP_NAME}, '')) % LOWER(:$NAME_OR_DESCRIPTION)")
            if (includeDescription) {
              append(" OR LOWER(COALESCE(${p.TABLE_ALIAS}.${p.DESCRIPTION}, '')) % LOWER(:$NAME_OR_DESCRIPTION)")
            }
          }
          context.whereClauses.add("($conditions)")
          context.params[NAME_OR_DESCRIPTION] = text
        }

        SearchStrategy.PREFIX -> {
          context.whereClauses.add(
            "(LOWER(${p.TABLE_ALIAS}.${p.NAME}) LIKE LOWER(:$NAME_OR_DESCRIPTION) " +
              "OR LOWER(COALESCE(${p.TABLE_ALIAS}.${p.PRODUCT_GROUP_NAME}, '')) LIKE LOWER(:$NAME_OR_DESCRIPTION))"
          )
          context.params[NAME_OR_DESCRIPTION] = "$text%"
        }

        SearchStrategy.WILDCARD -> {
          val conditions = buildString {
            append("LOWER(${p.TABLE_ALIAS}.${p.NAME}) LIKE LOWER(:$NAME_OR_DESCRIPTION) ")
            append("OR LOWER(COALESCE(${p.TABLE_ALIAS}.${p.PRODUCT_GROUP_NAME}, '')) LIKE LOWER(:$NAME_OR_DESCRIPTION)")
            if (includeDescription) {
              append(" OR LOWER(COALESCE(${p.TABLE_ALIAS}.${p.DESCRIPTION}, '')) LIKE LOWER(:$NAME_OR_DESCRIPTION)")
            }
          }
          context.whereClauses.add("($conditions)")
          context.params[NAME_OR_DESCRIPTION] = "%$text%"
        }

        SearchStrategy.NONE -> return
      }
    }
  }
}
