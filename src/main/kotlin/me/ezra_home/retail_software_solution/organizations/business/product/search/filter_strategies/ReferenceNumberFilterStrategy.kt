package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.Aliases
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames.REFERENCE_NUMBER

class ReferenceNumberFilterStrategy(
    private val referenceNumber: String?
) : FilterStrategy {

    override fun apply(context: QueryBuilderContext) {
        referenceNumber?.takeIf { it.isNotBlank() }?.let { ref ->
            val p = Aliases.ColumnNames.Product
            context.whereClauses.add("${p.TABLE_ALIAS}.${p.REFERENCE_NUMBER} LIKE :$REFERENCE_NUMBER")
            context.params[REFERENCE_NUMBER] = "$ref%"
        }
    }
}
