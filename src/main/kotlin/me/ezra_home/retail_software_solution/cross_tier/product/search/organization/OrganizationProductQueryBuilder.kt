package me.ezra_home.retail_software_solution.cross_tier.product.search.organization

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import me.ezra_home.retail_software_solution.util.queries.QueryMetadata
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.CategoryFilterStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.NameFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.ReferenceNumberFilterStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.StatusFilterStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.TextSearchFilterStrategy
import me.ezra_home.retail_software_solution.util.model.TableNames

internal object OrganizationProductQueryBuilder {

  private val P = Aliases.ColumnNames.CrossTierProduct
  private val ORG_P = Aliases.ColumnNames.OrganizationProduct
  private val PG = Aliases.ColumnNames.ProductGroup
  private val PT = Aliases.ColumnNames.ProductTag

  fun buildSearchQuery(
    searchParams: ProductSearchParameters,
    previousName: String
  ): SqlQuery {
    val context = QueryBuilderContext()
    val hasTagFilter = searchParams.tagIds.isNotEmpty()
    val hasCategoryFilter = searchParams.categoryIds.isNotEmpty()

    context.whereClauses.add("1=1")

    val statusCodes = searchParams.extractStatusCodes()
    applyFilters(context, searchParams, previousName, statusCodes)

    val sql = buildQuery(context, hasTagFilter, hasCategoryFilter)
    val metadata = buildMetadata(searchParams, statusCodes, hasTagFilter)

    return SqlQuery(sql, context.params, metadata)
  }

  private fun applyFilters(
    context: QueryBuilderContext,
    searchParams: ProductSearchParameters,
    previousName: String,
    statusCodes: Set<String>
  ) {
    NameFilterStrategy(previousName).apply(context)
    StatusFilterStrategy(statusCodes).apply(context)

    TextSearchFilterStrategy(searchParams.searchText, searchParams.searchStrategy).apply(context)
    ReferenceNumberFilterStrategy(searchParams.referenceNumber).apply(context)
    CategoryFilterStrategy(searchParams.categoryIds, PG.TABLE_ALIAS).apply(context)
    TagFilterStrategy(searchParams.tagIds).apply(context)
  }

  private fun buildQuery(context: QueryBuilderContext, hasTagFilter: Boolean, hasCategoryFilter: Boolean): String {
    return when {
      hasTagFilter -> buildTagFilteredQuery(context, hasCategoryFilter)
      else -> buildSimpleQuery(context, hasCategoryFilter)
    }
  }

  private fun buildSimpleQuery(context: QueryBuilderContext, hasCategoryFilter: Boolean): String {
    val joinClause = if (hasCategoryFilter) {
      "INNER JOIN ${TableNames.PRODUCT_GROUP} ${PG.TABLE_ALIAS} ON ${P.TABLE_ALIAS}.${ORG_P.PRODUCT_GROUP_ID} = ${PG.TABLE_ALIAS}.${PG.ID}"
    } else {
      ""
    }

    return """
      SELECT ${P.TABLE_ALIAS}.*
      FROM ${TableNames.PRODUCT} ${P.TABLE_ALIAS}
      $joinClause
      WHERE ${context.whereClauses.joinToString(" AND ")}
      ORDER BY LOWER(${P.TABLE_ALIAS}.${P.PRODUCT_NAME})
      LIMIT :${ParameterNames.PAGE_SIZE}
    """.trimIndent()
  }

  private fun buildTagFilteredQuery(context: QueryBuilderContext, hasCategoryFilter: Boolean): String {
    val (productFilters, tagFilters) = partitionFilters(context.whereClauses, hasCategoryFilter)

    val havingClause = if (context.havingClauses.isNotEmpty()) {
      "HAVING ${context.havingClauses.joinToString(" AND ")}"
    } else {
      ""
    }

    val productGroupJoin = if (hasCategoryFilter) {
      "INNER JOIN ${TableNames.PRODUCT_GROUP} ${PG.TABLE_ALIAS} ON ${P.TABLE_ALIAS}.${ORG_P.PRODUCT_GROUP_ID} = ${PG.TABLE_ALIAS}.${PG.ID}"
    } else {
      ""
    }

    return """
      SELECT ${P.TABLE_ALIAS}.*
      FROM (
        SELECT filtered_products.${P.ID}, filtered_products.${P.PRODUCT_NAME}
        FROM (
          SELECT ${P.TABLE_ALIAS}.${P.ID}, ${P.TABLE_ALIAS}.${P.PRODUCT_NAME}
          FROM ${TableNames.PRODUCT} ${P.TABLE_ALIAS}
          $productGroupJoin
          WHERE ${productFilters.joinToString(" AND ")}
        ) filtered_products
        INNER JOIN ${TableNames.PRODUCT_TAG} ${PT.TABLE_ALIAS} ON ${PT.TABLE_ALIAS}.${PT.PRODUCT_ID} = filtered_products.${P.ID}
        WHERE ${tagFilters.joinToString(" AND ")}
        GROUP BY filtered_products.${P.ID}, filtered_products.${P.PRODUCT_NAME}
        $havingClause
        ORDER BY LOWER(filtered_products.${P.PRODUCT_NAME})
        LIMIT :${ParameterNames.PAGE_SIZE}
      ) final_ids
      INNER JOIN ${TableNames.PRODUCT} ${P.TABLE_ALIAS} ON ${P.TABLE_ALIAS}.${P.ID} = final_ids.${P.ID}
      ORDER BY LOWER(${P.TABLE_ALIAS}.${P.PRODUCT_NAME})
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
  ): QueryMetadata {
    return QueryMetadata(
      categoryIdsCount = searchParams.categoryIds.size,
      tagIdsCount = searchParams.tagIds.size,
      statusListCount = statusCodes.size,
      hasTagFilter = hasTagFilter,
      hasTextSearch = !searchParams.searchText.isNullOrBlank(),
      hasReferenceNumberSearch = !searchParams.referenceNumber.isNullOrBlank()
    )
  }
}
