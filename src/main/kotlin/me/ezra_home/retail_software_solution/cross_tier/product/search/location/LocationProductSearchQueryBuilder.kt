package me.ezra_home.retail_software_solution.cross_tier.product.search.location

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import me.ezra_home.retail_software_solution.util.queries.QueryMetadata
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.CategoryFilterStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.NameFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.StatusFilterStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.TextSearchFilterStrategy
import me.ezra_home.retail_software_solution.util.model.TableNames

object LocationProductSearchQueryBuilder {

  private val P = Aliases.ColumnNames.Product

  fun buildSearchQuery(
    searchParams: ProductSearchParameters,
    previousName: String
  ): SqlQuery {
    val context = QueryBuilderContext()

    context.whereClauses.add("1=1")

    val statusCodes = searchParams.extractStatusCodes()
    applyFilters(context, searchParams, previousName, statusCodes)

    val sql = buildQuery(context)
    val metadata = buildMetadata(searchParams, statusCodes)

    return SqlQuery(sql, context.params, metadata)
  }

  private fun applyFilters(
      context: QueryBuilderContext,
      searchParams: ProductSearchParameters,
      previousName: String,
      statusCodes: Set<String>
  ) {
    NameFilterStrategy(previousName, P.TABLE_ALIAS).apply(context)
    StatusFilterStrategy(statusCodes, P.TABLE_ALIAS).apply(context)
    TextSearchFilterStrategy(searchParams.searchText, searchParams.searchStrategy, includeDescription = false).apply(context)
    CategoryFilterStrategy(searchParams.categoryIds, P.TABLE_ALIAS).apply(context)
  }

  private fun buildQuery(context: QueryBuilderContext): String {
    return """
      SELECT ${P.TABLE_ALIAS}.*
      FROM ${TableNames.LOCATION_PRODUCT} ${P.TABLE_ALIAS}
      WHERE ${context.whereClauses.joinToString(" AND ")}
      ORDER BY LOWER(${P.TABLE_ALIAS}.${P.NAME})
      LIMIT :${ParameterNames.PAGE_SIZE}
    """.trimIndent()
  }

  private fun buildMetadata(
    searchParams: ProductSearchParameters,
    statusCodes: Set<String>
  ): QueryMetadata {
    return QueryMetadata(
      queryName = "location_product_search",
      categoryIdsCount = searchParams.categoryIds.size,
      statusListCount = statusCodes.size,
      hasTextSearch = !searchParams.searchText.isNullOrBlank()
    )
  }
}
