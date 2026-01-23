package me.ezra_home.retail_software_solution.organizations.business.product.search

import jakarta.persistence.QueryTimeoutException
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product.ProductCache
import me.ezra_home.retail_software_solution.organizations.business.product.ProductMapper
import me.ezra_home.retail_software_solution.organizations.business.product.ProductService
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.Builder
import me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping.ProductTagQualifier
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class ProductSearchService(
  private val productMapper: ProductMapper,
  private val executor: ProductSearchExecutor,
  private val productCache: ProductCache,
  private val productService: ProductService,
  private val productTagQualifier: ProductTagQualifier
) {

  companion object {
    private const val REQUIRE_CLIENT_SIDE_FILTER_THRESHOLD = 1000
  }

  fun searchWithParameters(pageRequest: PageRequest<ProductSearchParameters, String>): PageResponse<ProductResponseDto, String> {

    if (shouldUseClientSideFiltering()) {
        return loadAllProductsForClientFiltering()
    }

    ProductSearchValidator.validateArraySizes(pageRequest.parameters)
    val searchText = pageRequest.parameters.searchText

    if (searchText.isNullOrBlank()) {
      return executeSingleSearch(
        pageRequest.parameters,
        pageRequest.previousCursor,
        pageRequest.requestedSize,
        false
      )
    }

    return trySearchStrategies(pageRequest, selectSearchStrategies(searchText))
  }

  private fun shouldUseClientSideFiltering(): Boolean {
    return productCache.countAllProducts() <= REQUIRE_CLIENT_SIDE_FILTER_THRESHOLD
  }

  private fun loadAllProductsForClientFiltering(): PageResponse<ProductResponseDto, String> {
    return PageResponse(
      currentCursor = "",
      hasMore = false,
      contents = productService.findAllProducts(),
      requireClientSideFilter = true
    )
  }

  private fun selectSearchStrategies(searchText: String): List<SearchMode> {
    return when {
      searchText.length <= 3 -> listOf(SearchMode.PREFIX, SearchMode.TRIGRAM)
      searchText.length <= 4 -> listOf(SearchMode.PREFIX, SearchMode.TRIGRAM, SearchMode.WILDCARD)
      searchText.length <= 6 -> listOf(SearchMode.FULLTEXT, SearchMode.TRIGRAM, SearchMode.PREFIX, SearchMode.WILDCARD)
      else -> listOf(SearchMode.FULLTEXT, SearchMode.TRIGRAM, SearchMode.PREFIX)
    }
  }

  private fun trySearchStrategies(
    pageRequest: PageRequest<ProductSearchParameters, String>,
    strategies: List<SearchMode>
  ): PageResponse<ProductResponseDto, String> {

    for (mode in strategies) {
      try {
        val result = executeSingleSearch(
          pageRequest.parameters,
          pageRequest.previousCursor,
          pageRequest.requestedSize,
          mode == SearchMode.WILDCARD
        )
        if (result.contents.isNotEmpty()) {
          return result
        }
      } catch (_: QueryTimeoutException) {
        // Continue to next strategy
      }
    }

    return PageResponse(pageRequest.previousCursor, false, emptyList())
  }

  private fun executeSingleSearch(
    params: ProductSearchParameters,
    previousName: String,
    pageSize: Int,
    setTimeout: Boolean = false,
  ): PageResponse<ProductResponseDto, String> {
    val sqlQuery = Builder.buildSearchQuery(params, previousName)
    val results: List<ProductEntity> = executor.executeQuery(sqlQuery, pageSize + 1, setTimeout)

    val hasMore = results.size > pageSize
    val pageResults = if (hasMore) results.take(pageSize) else results
    val dtos: Collection<ProductResponseDto> = pageResults.map { productMapper.toDtoWithoutTags(it) }
    val contents: Collection<ProductResponseDto> = productTagQualifier.populateTagsForProducts(dtos)
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
