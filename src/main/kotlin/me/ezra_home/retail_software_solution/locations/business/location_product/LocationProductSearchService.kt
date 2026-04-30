package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductSearchService(
  private val locationProductCache: LocationProductCache,
  private val locationProductEnricher: LocationProductEnricher,
  locationProductFetcher: LocationProductFetcher
) : ProductSearchService<LocationProductResponseDto>(
  locationProductFetcher,
  LocationProductSearchQueryBuilder::buildSearchQuery
) {

  override fun countAllProducts(): Long = locationProductCache.countAllLocationProducts()

  override fun findAllProducts(): List<LocationProductResponseDto> {
    return locationProductEnricher.provideMappingContext(locationProductCache.findAllLocationProducts())
  }
}
