package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.Aliases
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames.TAG_IDS
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames.TAG_IDS_COUNT
import java.util.UUID

/**
 * Filter strategy for tag-based filtering.
 *
 * This strategy enforces that products must have ALL specified tags (intersection, not union).
 * It adds:
 * - WHERE clauses to filter by tag IDs and active tags (end_on IS NULL)
 * - HAVING clause to ensure COUNT(DISTINCT tag_id) equals the number of tags specified
 *
 * This is the most complex filter as it requires GROUP BY aggregation in the query.
 */
class TagFilterStrategy(
    private val tagIds: Set<UUID>
) : FilterStrategy {

    override fun apply(context: QueryBuilderContext) {
        if (tagIds.isNotEmpty()) {
            val pt = Aliases.ColumnNames.ProductTag

            // Filter by tag IDs and active tags only
            context.whereClauses.add("${pt.TABLE_ALIAS}.${pt.TAG_ID} = ANY(:$TAG_IDS)")
            context.whereClauses.add("${pt.TABLE_ALIAS}.${pt.END_ON} IS NULL")

            // Ensure product has ALL specified tags (intersection)
            context.havingClauses.add("COUNT(DISTINCT ${pt.TABLE_ALIAS}.${pt.TAG_ID}) = :$TAG_IDS_COUNT")

            context.params[TAG_IDS] = tagIds.toTypedArray()
            context.params[TAG_IDS_COUNT] = tagIds.size
        }
    }
}
