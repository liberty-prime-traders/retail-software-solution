package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.SearchStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.Aliases
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames.NAME_OR_DESCRIPTION

class TextSearchFilterStrategy(
    private val searchText: String?,
    private val searchStrategy: SearchStrategy
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
                    context.whereClauses.add(
                        "(LOWER(${p.TABLE_ALIAS}.${p.NAME}) % LOWER(:$NAME_OR_DESCRIPTION) " +
                        "OR LOWER(COALESCE(${p.TABLE_ALIAS}.${p.DESCRIPTION}, '')) % LOWER(:$NAME_OR_DESCRIPTION) " +
                        "OR LOWER(COALESCE(${p.TABLE_ALIAS}.${p.PRODUCT_GROUP_NAME}, '')) % LOWER(:$NAME_OR_DESCRIPTION))"
                    )
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
                    context.whereClauses.add(
                        "(LOWER(${p.TABLE_ALIAS}.${p.NAME}) LIKE LOWER(:$NAME_OR_DESCRIPTION) " +
                        "OR LOWER(COALESCE(${p.TABLE_ALIAS}.${p.DESCRIPTION}, '')) LIKE LOWER(:$NAME_OR_DESCRIPTION) " +
                        "OR LOWER(COALESCE(${p.TABLE_ALIAS}.${p.PRODUCT_GROUP_NAME}, '')) LIKE LOWER(:$NAME_OR_DESCRIPTION))"
                    )
                    context.params[NAME_OR_DESCRIPTION] = "%$text%"
                }

                SearchStrategy.NONE -> return
            }
        }
    }
}
