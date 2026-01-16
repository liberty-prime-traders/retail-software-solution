package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.Aliases
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames.PREVIOUS_CURSOR

class CursorFilterStrategy(
    private val previousCursor: Long
) : FilterStrategy {

    override fun apply(context: QueryBuilderContext) {
        val p = Aliases.ColumnNames.Product
        context.whereClauses.add("${p.TABLE_ALIAS}.${p.CURSOR} > :$PREVIOUS_CURSOR")
        context.params[PREVIOUS_CURSOR] = previousCursor
    }
}
