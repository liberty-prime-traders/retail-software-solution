package me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder

import me.ezra_home.retail_software_solution.organizations.business.product.search.ProductSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.product.search.ProductSearchUtilityTypes
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.CategoryFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.CursorFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.QueryBuilderContext
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.ReferenceNumberFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.StatusFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.TagFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies.TextSearchFilterStrategy
import me.ezra_home.retail_software_solution.util.model.TableNames


object Builder {

    private val P = Aliases.ColumnNames.Product
    private val PG = Aliases.ColumnNames.ProductGroup
    private val PT = Aliases.ColumnNames.ProductTag

    fun buildSearchQuery(
        searchParams: ProductSearchParameters,
        previousCursor: Long
    ): ProductSearchUtilityTypes.SqlQuery {
        val context = QueryBuilderContext()
        val hasTagFilter = searchParams.tagsIds.isNotEmpty()
        val hasCategoryFilter = searchParams.categoryIds.isNotEmpty()

        // Ensures WHERE clause always exists (PostgreSQL optimizer eliminates this)
        context.whereClauses.add("1=1")

        val statusCodes = extractStatusCodes(searchParams)
        applyFilters(context, searchParams, previousCursor, statusCodes)

        val sql = buildQuery(context, hasTagFilter, hasCategoryFilter)
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

        TextSearchFilterStrategy(searchParams.searchText, searchParams.searchMode).apply(context)
        ReferenceNumberFilterStrategy(searchParams.referenceNumber).apply(context)
        CategoryFilterStrategy(searchParams.categoryIds).apply(context)
        TagFilterStrategy(searchParams.tagsIds).apply(context)
    }

    private fun extractStatusCodes(searchParams: ProductSearchParameters): Set<String> {
        return searchParams.statusList.map { it.code }.toSet()
    }

    private fun buildQuery(context: QueryBuilderContext, hasTagFilter: Boolean, hasCategoryFilter: Boolean): String {
        return when {
            hasTagFilter -> buildTagFilteredQuery(context, hasCategoryFilter)
            else -> buildSimpleQuery(context, hasCategoryFilter)
        }
    }

    private fun buildSimpleQuery(context: QueryBuilderContext, hasCategoryFilter: Boolean): String {
        val joinClause = if (hasCategoryFilter) {
            "INNER JOIN ${TableNames.PRODUCT_GROUP} ${PG.TABLE_ALIAS} ON ${P.TABLE_ALIAS}.${P.PRODUCT_GROUP_ID} = ${PG.TABLE_ALIAS}.${PG.ID}"
        } else {
            ""
        }

        return """
            SELECT ${P.TABLE_ALIAS}.*
            FROM ${TableNames.PRODUCT} ${P.TABLE_ALIAS}
            $joinClause
            WHERE ${context.whereClauses.joinToString(" AND ")}
            ORDER BY ${P.TABLE_ALIAS}.${P.CURSOR}
            LIMIT :${ParameterNames.PAGE_SIZE}
        """.trimIndent()
    }

    /**
     * Builds query with optimized subquery pattern for tag filtering.
     *
     * Key optimization: Filter products FIRST using composite indexes (status/cursor),
     * THEN join with product_tag. This ensures the join operates on a small filtered set
     * rather than the entire product_tag table.
     *
     * Query structure:
     * 1. Inner subquery: Filter products by non-tag criteria (uses composite indexes)
     * 2. Join with product_group if category filter is present
     * 3. Join filtered products with product_tag
     * 4. Apply tag filters and HAVING clause
     * 5. LIMIT the aggregated result
     * 6. Final join back to product table for all columns
     */
    private fun buildTagFilteredQuery(context: QueryBuilderContext, hasCategoryFilter: Boolean): String {
        val (productFilters, tagFilters) = partitionFilters(context.whereClauses, hasCategoryFilter)

        val havingClause = if (context.havingClauses.isNotEmpty()) {
            "HAVING ${context.havingClauses.joinToString(" AND ")}"
        } else {
            ""
        }

        val productGroupJoin = if (hasCategoryFilter) {
            "INNER JOIN ${TableNames.PRODUCT_GROUP} ${PG.TABLE_ALIAS} ON ${P.TABLE_ALIAS}.${P.PRODUCT_GROUP_ID} = ${PG.TABLE_ALIAS}.${PG.ID}"
        } else {
            ""
        }

        return """
            SELECT ${P.TABLE_ALIAS}.*
            FROM (
              SELECT filtered_products.${P.ID}, filtered_products.${P.CURSOR}
              FROM (
                SELECT ${P.TABLE_ALIAS}.${P.ID}, ${P.TABLE_ALIAS}.${P.CURSOR}
                FROM ${TableNames.PRODUCT} ${P.TABLE_ALIAS}
                $productGroupJoin
                WHERE ${productFilters.joinToString(" AND ")}
              ) filtered_products
              INNER JOIN ${TableNames.PRODUCT_TAG} ${PT.TABLE_ALIAS} ON ${PT.TABLE_ALIAS}.${PT.PRODUCT_ID} = filtered_products.${P.ID}
              WHERE ${tagFilters.joinToString(" AND ")}
              GROUP BY filtered_products.${P.ID}, filtered_products.${P.CURSOR}
              $havingClause
              ORDER BY filtered_products.${P.CURSOR}
              LIMIT :${ParameterNames.PAGE_SIZE}
            ) final_ids
            INNER JOIN ${TableNames.PRODUCT} ${P.TABLE_ALIAS} ON ${P.TABLE_ALIAS}.${P.ID} = final_ids.${P.ID}
            ORDER BY ${P.TABLE_ALIAS}.${P.CURSOR}
        """.trimIndent()
    }

    private fun partitionFilters(whereClauses: List<String>, hasCategoryFilter: Boolean): Pair<List<String>, List<String>> {
        val productFilters = mutableListOf<String>()
        val tagFilters = mutableListOf<String>()

        whereClauses.forEach { clause ->
            when {
                clause.contains("${PT.TABLE_ALIAS}.") -> tagFilters.add(clause)
                clause.contains("${PG.TABLE_ALIAS}.") && hasCategoryFilter -> productFilters.add(clause)
                else -> productFilters.add(clause)
            }
        }

        // Ensure we always have at least a dummy clause to avoid empty WHERE
        if (productFilters.isEmpty()) {
            productFilters.add("1=1")
        }
        if (tagFilters.isEmpty()) {
            tagFilters.add("1=1")
        }

        return Pair(productFilters, tagFilters)
    }

    private fun buildMetadata(
        searchParams: ProductSearchParameters,
        statusCodes: Set<String>,
        hasTagFilter: Boolean
    ): ProductSearchUtilityTypes.QueryMetadata {
        return ProductSearchUtilityTypes.QueryMetadata(
            categoryIdsCount = searchParams.categoryIds.size,
            tagIdsCount = searchParams.tagsIds.size,
            statusListCount = statusCodes.size,
            hasTagFilter = hasTagFilter,
            hasTextSearch = !searchParams.searchText.isNullOrBlank(),
            hasReferenceNumberSearch = !searchParams.referenceNumber.isNullOrBlank()
        )
    }
}
