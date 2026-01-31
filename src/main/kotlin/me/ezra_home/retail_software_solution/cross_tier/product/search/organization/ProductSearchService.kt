package me.ezra_home.retail_software_solution.cross_tier.product.search.organization

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.util.queries.QueryFormatter
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchValidator
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductFetcher
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.SearchStrategyRetryExecutor
import me.ezra_home.retail_software_solution.organizations.business.product.ProductCache
import me.ezra_home.retail_software_solution.organizations.business.product.ProductMapper
import me.ezra_home.retail_software_solution.organizations.business.product.ProductService
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
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
) : ProductFetcher<ProductResponseDto> {

  companion object {
    private const val REQUIRE_CLIENT_SIDE_FILTER_THRESHOLD = 1000
  }

  fun searchWithParameters(pageRequest: PageRequest<ProductSearchParameters, String>): PageResponse<ProductResponseDto, String> {

    if (shouldUseClientSideFiltering()) {
      return loadAllProductsForClientFiltering()
    }

    ProductSearchValidator.validateArraySizes(
      pageRequest.parameters.categoryIds,
      pageRequest.parameters.statusList,
      pageRequest.parameters.tagsIds
    )
    val searchText = pageRequest.parameters.searchText

    if (searchText.isNullOrBlank()) {
      return fetchProducts(pageRequest, false)
    }

    return trySearchStrategies(pageRequest, searchText)
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

  private fun trySearchStrategies(
    pageRequest: PageRequest<ProductSearchParameters, String>,
    searchText: String
  ): PageResponse<ProductResponseDto, String> {
    return SearchStrategyRetryExecutor.executeWithRetry(
      searchText = searchText,
      pageRequest = pageRequest,
      productFetcher = this
    )
  }

  override fun fetchProducts(
    pageRequest: PageRequest<ProductSearchParameters, String>,
    setTimeout: Boolean
  ): PageResponse<ProductResponseDto, String> {
    val sqlQuery = OrganizationProductQueryBuilder.buildSearchQuery(
      pageRequest.parameters,
      pageRequest.previousCursor
    )
    val results: List<ProductEntity> = executor.executeOrgQuery(sqlQuery, pageRequest.requestedSize + 1, setTimeout)

    val hasMore = results.size > pageRequest.requestedSize
    val pageResults = if (hasMore) results.take(pageRequest.requestedSize) else results
    val dtos: List<ProductResponseDto> = pageResults.map { productMapper.toDtoWithoutTags(it) }
    val contents: Collection<ProductResponseDto> = productTagQualifier.populateTagsForProducts(dtos)
    val currentCursor = contents.lastOrNull()?.productName ?: pageRequest.previousCursor

    return PageResponse(
      currentCursor = currentCursor,
      hasMore = hasMore,
      contents = contents
    )
  }

  fun generateFormattedQuery(pageRequest: PageRequest<ProductSearchParameters, String>): String {
    ProductSearchValidator.validateArraySizes(
      pageRequest.parameters.categoryIds,
      pageRequest.parameters.statusList,
      pageRequest.parameters.tagsIds
    )
    val sqlQuery = OrganizationProductQueryBuilder.buildSearchQuery(pageRequest.parameters, pageRequest.previousCursor)
    return QueryFormatter.formatQueryWithParameters(sqlQuery, pageRequest.requestedSize, ParameterNames.PAGE_SIZE)
  }
}
