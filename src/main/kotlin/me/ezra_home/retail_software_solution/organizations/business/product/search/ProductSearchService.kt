package me.ezra_home.retail_software_solution.organizations.business.product.search

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

    val searchText = pageRequest.parameters.productNameOrDescription

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

    if (searchText.length >= 3) {
      return executeQueryAndMapResults(
        pageRequest.parameters.copy(searchMode = SearchMode.WILDCARD),
        pageRequest.previousCursor,
        pageSize
      )
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
    pageSize: Int
  ): PageResponse<ProductResponseDto> {
    val sqlQuery = Builder.buildSearchQuery(params, previousCursor)
    val results: List<ProductEntity> = executor.executeQuery(sqlQuery, pageSize + 1)

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
}
