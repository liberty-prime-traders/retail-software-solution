package me.ezra_home.retail_software_solution.cross_tier.product.search.location

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.dto.LocationProductResponseDto
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
