package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.CategoryFilterStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.NameFilterStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.ReferenceNumberFilterStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.StatusFilterStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.TextSearchFilterStrategy
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.QueryMetadata
import me.ezra_home.retail_software_solution.util.queries.SqlQuery

object LocationProductSearchQueryBuilder {

  private val P = Aliases.ColumnNames.CrossTierProduct

  fun buildSearchQuery(searchParams: ProductSearchParameters, previousName: String): SqlQuery {
    val context = QueryBuilderContext()

    context.whereClauses.add("1=1")

    applyFilters(context, searchParams, previousName)

    val sql = buildQuery(context)
    val metadata = buildMetadata(searchParams)

    return SqlQuery(sql, context.params, metadata)
  }

  private fun applyFilters(context: QueryBuilderContext, searchParams: ProductSearchParameters, previousName: String) {
    NameFilterStrategy(previousName).apply(context)
    StatusFilterStrategy(searchParams.extractStatusCodes()).apply(context)
    ReferenceNumberFilterStrategy(searchParams.referenceNumber).apply(context)
    TextSearchFilterStrategy(searchParams.searchText, searchParams.searchStrategy).apply(context)
    CategoryFilterStrategy(searchParams.categoryIds, P.TABLE_ALIAS).apply(context)
  }

  private fun buildQuery(context: QueryBuilderContext): String {
    return """
      SELECT ${P.TABLE_ALIAS}.*
      FROM ${TableNames.LOCATION_PRODUCT} ${P.TABLE_ALIAS}
      WHERE ${context.whereClauses.joinToString(" AND ")}
      ORDER BY LOWER(${P.TABLE_ALIAS}.${P.PRODUCT_NAME})
      LIMIT :${ParameterNames.PAGE_SIZE}
    """.trimIndent()
  }

  private fun buildMetadata(searchParams: ProductSearchParameters): QueryMetadata {
    return QueryMetadata(
      queryName = "location_product_search",
      categoryIdsCount = searchParams.categoryIds.size,
      statusListCount = searchParams.extractStatusCodes().size,
      hasTextSearch = !searchParams.searchText.isNullOrBlank()
    )
  }
}
