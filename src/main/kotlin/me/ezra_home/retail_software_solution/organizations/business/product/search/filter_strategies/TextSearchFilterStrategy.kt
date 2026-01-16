package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.SearchMode
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.Aliases
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames.NAME_OR_DESCRIPTION

class TextSearchFilterStrategy(
    private val searchText: String?,
    private val searchMode: SearchMode
) : FilterStrategy {

    override fun apply(context: QueryBuilderContext) {
        searchText?.takeIf { it.isNotBlank() }?.let { text ->
            if (searchMode == SearchMode.NONE) return

            val p = Aliases.ColumnNames.Product

            when (searchMode) {
                SearchMode.FULLTEXT -> {
                    context.whereClauses.add(
                        "${p.TABLE_ALIAS}.${p.SEARCH_VECTOR} @@ plainto_tsquery('english', :$NAME_OR_DESCRIPTION)"
                    )
                    context.params[NAME_OR_DESCRIPTION] = text
                }

                SearchMode.TRIGRAM -> {
                    context.whereClauses.add(
                        "(LOWER(${p.TABLE_ALIAS}.${p.NAME}) || ' ' || LOWER(COALESCE(${p.TABLE_ALIAS}.${p.DESCRIPTION}, ''))) " +
                        "% LOWER(:$NAME_OR_DESCRIPTION)"
                    )
                    context.params[NAME_OR_DESCRIPTION] = text
                }

                SearchMode.PREFIX -> {
                    context.whereClauses.add(
                        "(${p.TABLE_ALIAS}.${p.NAME} ILIKE :$NAME_OR_DESCRIPTION " +
                        "OR COALESCE(${p.TABLE_ALIAS}.${p.DESCRIPTION}, '') ILIKE :$NAME_OR_DESCRIPTION)"
                    )
                    context.params[NAME_OR_DESCRIPTION] = "$text%"
                }

                SearchMode.WILDCARD -> {
                    context.whereClauses.add(
                        "(${p.TABLE_ALIAS}.${p.NAME} ILIKE :$NAME_OR_DESCRIPTION " +
                        "OR COALESCE(${p.TABLE_ALIAS}.${p.DESCRIPTION}, '') ILIKE :$NAME_OR_DESCRIPTION)"
                    )
                    context.params[NAME_OR_DESCRIPTION] = "%$text%"
                }

                SearchMode.NONE -> return
            }
        }
    }
}
