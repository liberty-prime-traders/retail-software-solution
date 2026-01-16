package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.Aliases
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames.CATEGORY_IDS
import java.util.UUID

class CategoryFilterStrategy(
    private val categoryIds: Set<UUID>
) : FilterStrategy {

    override fun apply(context: QueryBuilderContext) {
        if (categoryIds.isNotEmpty()) {
            val p = Aliases.ColumnNames.Product
            context.whereClauses.add("${p.TABLE_ALIAS}.${p.CATEGORY_ID} = ANY(:$CATEGORY_IDS)")
            context.params[CATEGORY_IDS] = categoryIds.toTypedArray()
        }
    }
}
