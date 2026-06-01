package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductPagedSearch(
    private val locationProductCache: LocationProductCache,
    locationProductDtoFetcher: LocationProductDtoFetcher,
) : ProductSearchService<LocationProductDto>(locationProductDtoFetcher, LocationProductSearchQueryBuilder::buildSearchQuery) {

    override fun countAllProducts(): Long = locationProductCache.countAllLocationProducts()

    override fun findAllProducts(): List<LocationProductDto> =
        locationProductCache.findAllLocationProducts().filter { it.status == ProductStatus.ACTIVE }
}
