package me.ezra_home.retail_software_solution.cross_tier.product.search.organization

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchValidator
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductService
import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductResponseDto
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.QueryFormatter
import me.ezra_home.retail_software_solution.util.queries.SearchStrategyExecutor
import org.springframework.stereotype.Service

@Service
class OrganizationProductSearchService(
  private val organizationProductService: OrganizationProductService,
  private val organizationProductFetcher: OrganizationProductFetcher,
){

  fun searchWithParameters(
    pageRequest: PageRequest<ProductSearchParameters, String>
  ): PageResponse<OrganizationProductResponseDto, String> {

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
      return organizationProductFetcher.fetch(pageRequest, false)
    }

    return SearchStrategyExecutor.executeWithRetry(
      searchText = searchText,
      pageRequest = pageRequest,
      fetcher = organizationProductFetcher
    )
  }

  private fun shouldUseClientSideFiltering(): Boolean {
    return organizationProductService.countAllProducts() <= PageRequest.REQUIRE_CLIENT_SIDE_FILTER_THRESHOLD
  }

  private fun loadAllProductsForClientFiltering(): PageResponse<OrganizationProductResponseDto, String> {
    return PageResponse(
      currentCursor = "",
      hasMore = false,
      contents = organizationProductService.findAllProducts(),
      requireClientSideFilter = true
    )
  }

  fun generateFormattedQuery(pageRequest: PageRequest<ProductSearchParameters, String>): String {
    ProductSearchValidator.validateArraySizes(
      pageRequest.parameters.categoryIds,
      pageRequest.parameters.statusList,
      pageRequest.parameters.tagIds
    )
    val sqlQuery = OrganizationProductQueryBuilder.buildSearchQuery(pageRequest.parameters, pageRequest.previousCursor)
    return QueryFormatter.formatQueryWithParameters(sqlQuery, pageRequest.requestedSize, ParameterNames.PAGE_SIZE)
  }
}
