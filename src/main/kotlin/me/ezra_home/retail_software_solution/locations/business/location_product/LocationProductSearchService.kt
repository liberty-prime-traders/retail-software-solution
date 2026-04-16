package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import org.springframework.stereotype.Service

@Service
class LocationProductSearchService(
  private val locationProductService: LocationProductService,
  locationProductFetcher: LocationProductFetcher
) : ProductSearchService<LocationProductResponseDto>(
  locationProductFetcher,
  LocationProductSearchQueryBuilder::buildSearchQuery
) {

  override fun countAllProducts(): Long = locationProductService.countAllProducts()

  override fun findAllProducts(): List<LocationProductResponseDto> = locationProductService.findAllProducts()
}
