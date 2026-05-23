package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse

abstract class LocationProductTypedDataFetcher<DTO>(
    private val searchService: ProductSearchService<DTO>,
) {

    fun search(pageRequest: PageRequest<LocationProductSearchParameters, String>): PageResponse<DTO, String> {
        return searchService.searchWithParameters(toUnderlyingPageRequest(pageRequest))
    }

    private fun toUnderlyingPageRequest(
        pageRequest: PageRequest<LocationProductSearchParameters, String>
    ): PageRequest<ProductSearchParameters, String> = PageRequest(
        previousCursor = pageRequest.previousCursor,
        requestedSize = pageRequest.requestedSize,
        parameters = ProductSearchParameters(
            searchText = pageRequest.parameters.searchText,
            excludeIds = pageRequest.parameters.excludeIds,
            statusList = setOf(ProductStatus.ACTIVE),
            searchStrategy = pageRequest.parameters.searchStrategy
        )
    )
}
