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

  fun searchWithParameters(pageRequest: PageRequest<ProductSearchParameters>): PageResponse<ProductResponseDto> {
    val pageSize = pageRequest.requestedSize
    ProductSearchValidator.validateArraySizes(pageRequest.parameters)

    val searchText = pageRequest.parameters.searchText

    if (searchText.isNullOrBlank()) {
      return executeQueryAndMapResults(
        pageRequest.parameters.copy(searchMode = SearchMode.NONE),
        pageRequest.previousCursor,
        pageSize
      )
    }

    // Stage 1: Full-text search (fastest for word matches)
    val fullTextResults = executeQueryAndMapResults(
      pageRequest.parameters.copy(searchMode = SearchMode.FULLTEXT),
      pageRequest.previousCursor,
      pageSize
    )
    if (fullTextResults.contents.isNotEmpty()) {
      return fullTextResults
    }

    val trigramResults = executeQueryAndMapResults(
      pageRequest.parameters.copy(searchMode = SearchMode.TRIGRAM),
      pageRequest.previousCursor,
      pageSize
    )
    if (trigramResults.contents.isNotEmpty()) {
      return trigramResults
    }

    val prefixResults = executeQueryAndMapResults(
      pageRequest.parameters.copy(searchMode = SearchMode.PREFIX),
      pageRequest.previousCursor,
      pageSize
    )
    if (prefixResults.contents.isNotEmpty()) {
      return prefixResults
    }

    if (searchText.length in 3..6) {
      return try{
        executeQueryAndMapResults(
          pageRequest.parameters.copy(searchMode = SearchMode.WILDCARD),
          pageRequest.previousCursor,
          pageSize,
          setTimeout = true
        )
      } catch (e: QueryTimeoutException) {
        PageResponse(
          currentCursor = pageRequest.previousCursor,
          hasMore = false,
          contents = emptyList()
        )
      }
    }

    return PageResponse(
      currentCursor = pageRequest.previousCursor,
      hasMore = false,
      contents = emptyList()
    )
  }

  private fun executeQueryAndMapResults(
    params: ProductSearchParameters,
    previousCursor: Long,
    pageSize: Int,
    setTimeout: Boolean = false,
  ): PageResponse<ProductResponseDto> {
    val sqlQuery = Builder.buildSearchQuery(params, previousCursor)
    val results: List<ProductEntity> = executor.executeQuery(sqlQuery, pageSize + 1, setTimeout)

    val hasMore = results.size > pageSize
    val pageResults = if (hasMore) results.take(pageSize) else results
    val contents: Collection<ProductResponseDto> = pageResults.map { productMapper.toDto(it) }
    val currentCursor = contents.lastOrNull()?.cursor ?: previousCursor

    return PageResponse(
      currentCursor = currentCursor,
      hasMore = hasMore,
      contents = contents
    )
  }

  fun generateFormattedQuery(pageRequest: PageRequest<ProductSearchParameters>): String {
    ProductSearchValidator.validateArraySizes(pageRequest.parameters)
    val sqlQuery = Builder.buildSearchQuery(pageRequest.parameters, pageRequest.previousCursor)
    return ProductSearchQueryFormatter.formatQueryWithParameters(sqlQuery, pageRequest.requestedSize)
  }
}
