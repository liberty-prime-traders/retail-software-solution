package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.Aliases
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames.STATUS_LIST

/**
 * Filter strategy for product status filtering.
 * Adds a WHERE clause to filter products by status codes.
 */
class StatusFilterStrategy(
    private val statusCodes: Set<String>
) : FilterStrategy {

    override fun apply(context: QueryBuilderContext) {
        if (statusCodes.isNotEmpty()) {
            val p = Aliases.ColumnNames.Product
            context.whereClauses.add("${p.TABLE_ALIAS}.${p.STATUS} = ANY(:$STATUS_LIST)")
            context.params[STATUS_LIST] = statusCodes.toTypedArray()
        }
    }
}
