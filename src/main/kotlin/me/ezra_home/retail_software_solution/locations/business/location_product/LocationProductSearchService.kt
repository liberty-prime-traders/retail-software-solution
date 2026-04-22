package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductSearchService(
  private val locationProductCache: LocationProductCache,
  private val locationProductEnricher: LocationProductEnricher,
  private val locationProductRepository: LocationProductRepository,
  locationProductFetcher: LocationProductFetcher
) : ProductSearchService<LocationProductResponseDto>(
  locationProductFetcher,
  LocationProductSearchQueryBuilder::buildSearchQuery
) {

  override fun countAllProducts(): Long = locationProductCache.countAllLocationProducts()

  override fun findAllProducts(): List<LocationProductResponseDto> {
    return locationProductEnricher.provideMappingContext(locationProductCache.findAllLocationProducts())
  }

  fun guardAllActive(ids: Collection<UUID>) {
    val inactive = locationProductRepository.findAllById(ids)
      .filter { it.status != ProductStatus.ACTIVE }
    if (inactive.isNotEmpty()) throw RtsGenericException(
      "Inactive products are not allowed: ${inactive.map { it.id }}"
    )
  }
}
