package me.ezra_home.retail_software_solution.util.queries

import jakarta.persistence.QueryTimeoutException
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse

object SearchStrategyExecutor {

  fun <PARAMETERS: HasSearchStrategy<PARAMETERS>, RESPONSE_TYPE> executeWithRetry(
      searchText: String,
      pageRequest: PageRequest<PARAMETERS, String>,
      fetcher: FetchesUsingSmartTextStrategy<PARAMETERS, RESPONSE_TYPE>
  ): PageResponse<RESPONSE_TYPE, String> {
    for (strategy in selectSearchStrategies(searchText)) {
      try {
        val requestForStrategy = pageRequest.copy(
            parameters = pageRequest.parameters.withSearchStrategy(strategy)
        )
        val result = fetcher.fetch(requestForStrategy, strategy == SearchStrategy.WILDCARD)
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
