package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductForSaleSearchService
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductForSaleSearchService(
    private val locationProductForSaleSearchService: LocationProductForSaleSearchService,
) {

    fun search(
        pageRequest: PageRequest<SaleProductSearchParameters, String>
    ): PageResponse<LocationProductForSaleDto, String> {
        val page = locationProductForSaleSearchService.searchWithParameters(toUnderlyingPageRequest(pageRequest))
        val sellable = page.contents.filter { it.defaultSalePrice != null }
        return PageResponse(
            currentCursor = page.currentCursor,
            hasMore = page.hasMore,
            contents = sellable,
            requireClientSideFilter = page.requireClientSideFilter
        )
    }

    private fun toUnderlyingPageRequest(
        pageRequest: PageRequest<SaleProductSearchParameters, String>
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
