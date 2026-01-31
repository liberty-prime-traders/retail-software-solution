package me.ezra_home.retail_software_solution.cross_tier.product.search.location

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchValidator
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductFetcher
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.SearchStrategyRetryExecutor
import me.ezra_home.retail_software_solution.locations.model.LocationProductEntity
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductSearchService(
  private val executor: ProductSearchExecutor
) : ProductFetcher<LocationProductEntity> {

  fun searchWithParameters(
    pageRequest: PageRequest<ProductSearchParameters, String>
  ): PageResponse<LocationProductEntity, String> {

    ProductSearchValidator.validateArraySizes(
      pageRequest.parameters.categoryIds,
      pageRequest.parameters.statusList
    )
    val searchText = pageRequest.parameters.searchText

    if (searchText.isNullOrBlank()) {
      return fetchProducts(pageRequest, false)
    }

    return SearchStrategyRetryExecutor.executeWithRetry(
      searchText = searchText,
      pageRequest = pageRequest,
      productFetcher = this
    )
  }

  override fun fetchProducts(
    pageRequest: PageRequest<ProductSearchParameters, String>,
    setTimeout: Boolean
  ): PageResponse<LocationProductEntity, String> {
    val sqlQuery = LocationProductSearchQueryBuilder.buildSearchQuery(
      pageRequest.parameters,
      pageRequest.previousCursor
    )
    val results: List<LocationProductEntity> = executor.executeLocationQuery(
      sqlQuery,
      pageRequest.requestedSize + 1,
      setTimeout
    )

    val hasMore = results.size > pageRequest.requestedSize
    val pageResults = if (hasMore) results.take(pageRequest.requestedSize) else results
    val currentCursor = pageResults.lastOrNull()?.name ?: pageRequest.previousCursor

    return PageResponse(
      currentCursor = currentCursor,
      hasMore = hasMore,
      contents = pageResults
    )
  }
}
