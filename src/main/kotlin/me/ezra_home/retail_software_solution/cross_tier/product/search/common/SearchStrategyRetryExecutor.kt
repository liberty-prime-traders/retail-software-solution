package me.ezra_home.retail_software_solution.cross_tier.product.search.common

import jakarta.persistence.QueryTimeoutException
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.SearchStrategy

object SearchStrategyRetryExecutor {

  fun <T> executeWithRetry(
    searchText: String,
    pageRequest: PageRequest<ProductSearchParameters, String>,
    productFetcher: ProductFetcher<T>
  ): PageResponse<T, String> {
    for (strategy in selectSearchStrategies(searchText)) {
      try {
        val result = productFetcher.fetchProducts(pageRequest, strategy == SearchStrategy.WILDCARD)
        if (result.contents.isNotEmpty()) {
          return result
        }
      } catch (_: QueryTimeoutException) {
      }
    }
    return PageResponse(pageRequest.previousCursor, false, emptyList())
  }

  private fun selectSearchStrategies(searchText: String): List<SearchStrategy> {
    return when {
      searchText.length <= 3 -> listOf(SearchStrategy.PREFIX, SearchStrategy.TRIGRAM)
      searchText.length <= 4 -> listOf(SearchStrategy.PREFIX, SearchStrategy.TRIGRAM, SearchStrategy.WILDCARD)
      searchText.length <= 6 -> listOf(SearchStrategy.FULLTEXT, SearchStrategy.TRIGRAM, SearchStrategy.PREFIX, SearchStrategy.WILDCARD)
      else -> listOf(SearchStrategy.FULLTEXT, SearchStrategy.TRIGRAM, SearchStrategy.PREFIX)
    }
  }
}
