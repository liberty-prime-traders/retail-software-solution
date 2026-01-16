package me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder

import me.ezra_home.retail_software_solution.organizations.business.product.search.ProductSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.product.search.ProductSearchUtilityTypes
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.CategoryFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.CursorFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.ProductGroupFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.QueryBuilderContext
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.ReferenceNumberFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.StatusFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.TagFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.TextSearchFilterStrategy
import me.ezra_home.retail_software_solution.util.model.TableNames


object Builder {

    private val P = Aliases.ColumnNames.Product
    private val PT = Aliases.ColumnNames.ProductTag

    fun buildSearchQuery(
        searchParams: ProductSearchParameters,
        previousCursor: Long
    ): ProductSearchUtilityTypes.SqlQuery {
        val context = QueryBuilderContext()
        val hasTagFilter = searchParams.tagsIds.isNotEmpty()

        // Ensures WHERE clause always exists (PostgreSQL optimizer eliminates this)
        context.whereClauses.add("1=1")

        val statusCodes = extractStatusCodes(searchParams)
        applyFilters(context, searchParams, previousCursor, statusCodes)

        val sql = buildQuery(context, hasTagFilter)
        val metadata = buildMetadata(searchParams, statusCodes, hasTagFilter)

        return ProductSearchUtilityTypes.SqlQuery(sql, context.params, metadata)
    }

    private fun applyFilters(
        context: QueryBuilderContext,
        searchParams: ProductSearchParameters,
        previousCursor: Long,
        statusCodes: Set<String>
    ) {
        // Order matters: cursor and status are always present
        CursorFilterStrategy(previousCursor).apply(context)
        StatusFilterStrategy(statusCodes).apply(context)

        TextSearchFilterStrategy(searchParams.productNameOrDescription, searchParams.searchMode).apply(context)
        ReferenceNumberFilterStrategy(searchParams.referenceNumber).apply(context)
        CategoryFilterStrategy(searchParams.categoryIds).apply(context)
        ProductGroupFilterStrategy(searchParams.productGroupIds).apply(context)
        TagFilterStrategy(searchParams.tagsIds).apply(context)
    }

    private fun extractStatusCodes(searchParams: ProductSearchParameters): Set<String> {
        return searchParams.statusList.map { it.code }.toSet()
    }

    private fun buildQuery(context: QueryBuilderContext, hasTagFilter: Boolean): String {
        return if (hasTagFilter) {
            buildTagFilteredQuery(context)
        } else {
            buildSimpleQuery(context)
        }
    }

    private fun buildSimpleQuery(context: QueryBuilderContext): String {
        return """
            SELECT ${P.TABLE_ALIAS}.*
            FROM ${TableNames.PRODUCT} ${P.TABLE_ALIAS}
            WHERE ${context.whereClauses.joinToString(" AND ")}
            ORDER BY ${P.TABLE_ALIAS}.${P.CURSOR}
            LIMIT :${ParameterNames.PAGE_SIZE}
        """.trimIndent()
    }

    /**
     * Builds query with subquery pattern for tag filtering.
     *
     * Uses subquery to GROUP BY + HAVING + LIMIT early, then joins back to product table
     * to fetch all columns. This avoids aggregating the full dataset before limiting.
     */
    private fun buildTagFilteredQuery(context: QueryBuilderContext): String {
        val havingClause = if (context.havingClauses.isNotEmpty()) {
            "HAVING ${context.havingClauses.joinToString(" AND ")}"
        } else {
            ""
        }

        return """
            SELECT ${P.TABLE_ALIAS}.*
            FROM (
              SELECT ${P.TABLE_ALIAS}.${P.ID}, ${P.TABLE_ALIAS}.${P.CURSOR}
              FROM ${TableNames.PRODUCT} ${P.TABLE_ALIAS}
              INNER JOIN ${TableNames.PRODUCT_TAG} ${PT.TABLE_ALIAS} ON ${PT.TABLE_ALIAS}.${PT.PRODUCT_ID} = ${P.TABLE_ALIAS}.${P.ID}
              WHERE ${context.whereClauses.joinToString(" AND ")}
              GROUP BY ${P.TABLE_ALIAS}.${P.ID}, ${P.TABLE_ALIAS}.${P.CURSOR}
              $havingClause
              ORDER BY ${P.TABLE_ALIAS}.${P.CURSOR}
              LIMIT :${ParameterNames.PAGE_SIZE}
            ) filtered_ids
            INNER JOIN ${TableNames.PRODUCT} ${P.TABLE_ALIAS} ON ${P.TABLE_ALIAS}.${P.ID} = filtered_ids.${P.ID}
            ORDER BY ${P.TABLE_ALIAS}.${P.CURSOR}
        """.trimIndent()
    }

    private fun buildMetadata(
        searchParams: ProductSearchParameters,
        statusCodes: Set<String>,
        hasTagFilter: Boolean
    ): ProductSearchUtilityTypes.QueryMetadata {
        return ProductSearchUtilityTypes.QueryMetadata(
            categoryIdsCount = searchParams.categoryIds.size,
            productGroupIdsCount = searchParams.productGroupIds.size,
            tagIdsCount = searchParams.tagsIds.size,
            statusListCount = statusCodes.size,
            hasTagFilter = hasTagFilter,
            hasTextSearch = !searchParams.productNameOrDescription.isNullOrBlank(),
            hasReferenceNumberSearch = !searchParams.referenceNumber.isNullOrBlank()
        )
    }
}
