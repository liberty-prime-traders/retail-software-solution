package me.ezra_home.retail_software_solution.organizations.business.product.search

import jakarta.persistence.QueryTimeoutException
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product.ProductMapper
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.Builder
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class ProductSearchService(
  private val productMapper: ProductMapper,
  private val executor: ProductSearchExecutor
) {

  fun searchWithParameters(pageRequest: PageRequest<ProductSearchParameters, String>): PageResponse<ProductResponseDto, String> {
    ProductSearchValidator.validateArraySizes(pageRequest.parameters)
    val searchText = pageRequest.parameters.searchText

    if (searchText.isNullOrBlank()) {
      return executeQuery(pageRequest.parameters, pageRequest.previousCursor, pageRequest.requestedSize, SearchMode.NONE)
    }

    val searchModes = when {
      searchText.length <= 3 -> listOf(SearchMode.PREFIX, SearchMode.TRIGRAM)
      searchText.length <= 4 -> listOf(SearchMode.PREFIX, SearchMode.TRIGRAM, SearchMode.WILDCARD)
      searchText.length <= 6 -> listOf(SearchMode.FULLTEXT, SearchMode.TRIGRAM, SearchMode.PREFIX, SearchMode.WILDCARD)
      else -> listOf(SearchMode.FULLTEXT, SearchMode.TRIGRAM, SearchMode.PREFIX)
    }

    searchModes.forEach { mode ->
      try {
        val result = executeQuery(
          pageRequest.parameters,
          pageRequest.previousCursor,
          pageRequest.requestedSize,
          mode
        )
        if (result.contents.isNotEmpty()) return result
      } catch (_: QueryTimeoutException) {
      }
    }

    return PageResponse(pageRequest.previousCursor, false, emptyList())
  }

  private fun executeQuery(
    params: ProductSearchParameters,
    previousName: String,
    pageSize: Int,
    searchMode: SearchMode
  ): PageResponse<ProductResponseDto, String> {
    return executeQueryAndMapResults(params.copy(searchMode = searchMode), previousName, pageSize, searchMode == SearchMode.WILDCARD)
  }

  private fun executeQueryAndMapResults(
    params: ProductSearchParameters,
    previousName: String,
    pageSize: Int,
    setTimeout: Boolean = false,
  ): PageResponse<ProductResponseDto, String> {
    val sqlQuery = Builder.buildSearchQuery(params, previousName)
    val results: List<ProductEntity> = executor.executeQuery(sqlQuery, pageSize + 1, setTimeout)

    val hasMore = results.size > pageSize
    val pageResults = if (hasMore) results.take(pageSize) else results
    val contents: Collection<ProductResponseDto> = pageResults.map { productMapper.toDto(it) }
    val currentCursor = contents.lastOrNull()?.productName ?: previousName

    return PageResponse(
      currentCursor = currentCursor,
      hasMore = hasMore,
      contents = contents
    )
  }

  fun generateFormattedQuery(pageRequest: PageRequest<ProductSearchParameters, String>): String {
    ProductSearchValidator.validateArraySizes(pageRequest.parameters)
    val sqlQuery = Builder.buildSearchQuery(pageRequest.parameters, pageRequest.previousCursor)
    return ProductSearchQueryFormatter.formatQueryWithParameters(sqlQuery, pageRequest.requestedSize)
  }
}
