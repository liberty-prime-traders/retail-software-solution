package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductPagedSearch(
    private val locationProductCache: LocationProductCache,
    locationProductDtoFetcher: LocationProductDtoFetcher,
    @Value("\${rts.product-search.client-side-filter-threshold}") clientSideFilterThreshold: Int,
) : ProductSearchService<LocationProductDto>(
    locationProductDtoFetcher,
    LocationProductSearchQueryBuilder::buildSearchQuery,
    clientSideFilterThreshold
) {

    override fun countAllProducts(): Long = locationProductCache.countAllLocationProducts()

    override fun findAllProducts(parameters: ProductSearchParameters): List<LocationProductDto> =
        locationProductCache.findAllLocationProducts().filter { it.status in parameters.statusList }
}
