package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.Aliases
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames.PRODUCT_GROUP_IDS
import java.util.UUID

class ProductGroupFilterStrategy(
    private val productGroupIds: Set<UUID>
) : FilterStrategy {

    override fun apply(context: QueryBuilderContext) {
        if (productGroupIds.isNotEmpty()) {
            val p = Aliases.ColumnNames.Product
            context.whereClauses.add("${p.TABLE_ALIAS}.${p.PRODUCT_GROUP_ID} = ANY(:$PRODUCT_GROUP_IDS)")
            context.params[PRODUCT_GROUP_IDS] = productGroupIds.toTypedArray()
        }
    }
}
