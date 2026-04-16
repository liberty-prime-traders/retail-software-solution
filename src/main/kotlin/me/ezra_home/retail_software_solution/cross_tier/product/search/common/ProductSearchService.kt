package me.ezra_home.retail_software_solution.cross_tier.product.search.common

import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.FetchesUsingSmartTextStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryFormatter
import me.ezra_home.retail_software_solution.util.queries.SearchStrategyExecutor
import me.ezra_home.retail_software_solution.util.queries.SqlQuery

abstract class ProductSearchService<DTO>(
  private val fetcher: FetchesUsingSmartTextStrategy<ProductSearchParameters, DTO>,
  private val queryBuilder: (ProductSearchParameters, String) -> SqlQuery
) {

  protected abstract fun countAllProducts(): Long
  protected abstract fun findAllProducts(): List<DTO>

  fun searchWithParameters(pageRequest: PageRequest<ProductSearchParameters, String>): PageResponse<DTO, String> {

    if (shouldUseClientSideFiltering()) {
      return loadAllProductsForClientFiltering()
    }

    ProductSearchValidator.validateArraySizes(
      pageRequest.parameters.categoryIds,
      pageRequest.parameters.statusList,
      pageRequest.parameters.tagIds
    )
    val searchText = pageRequest.parameters.searchText

    if (searchText.isNullOrBlank()) {
      return fetcher.fetch(pageRequest, false)
    }

    return SearchStrategyExecutor.executeWithRetry(
      searchText = searchText,
      pageRequest = pageRequest,
      fetcher = fetcher
    )
  }

  private fun shouldUseClientSideFiltering(): Boolean {
    return countAllProducts() <= PageRequest.REQUIRE_CLIENT_SIDE_FILTER_THRESHOLD
  }

  private fun loadAllProductsForClientFiltering(): PageResponse<DTO, String> {
    return PageResponse(
      currentCursor = "",
      hasMore = false,
      contents = findAllProducts(),
      requireClientSideFilter = true
    )
  }

  fun generateFormattedQuery(pageRequest: PageRequest<ProductSearchParameters, String>): String {
    ProductSearchValidator.validateArraySizes(
      pageRequest.parameters.categoryIds,
      pageRequest.parameters.statusList,
      pageRequest.parameters.tagIds
    )
    val sqlQuery = queryBuilder(pageRequest.parameters, pageRequest.previousCursor)
    return QueryFormatter.formatQueryWithParameters(sqlQuery, pageRequest.requestedSize, ParameterNames.PAGE_SIZE)
  }
}
