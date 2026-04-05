package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.locations.model.LocationProductEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * This should not always be used unless the count has been called and
 * determined that the number of location products is small enough to be cached in memory.
 * This is to avoid caching a large number of location products in memory which can lead to performance issues.
 */
@Service
@CacheSchemaLevel(SchemaLevel.LOCATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION_PRODUCT])
internal class LocationProductCache(private val locationProductRepository: LocationProductRepository) {

  @Cacheable
  fun findAllLocationProducts(): List<LocationProductEntity> = locationProductRepository.findAllLocationProducts()

  @Cacheable
  fun countAllLocationProducts(): Long = locationProductRepository.count()

  @CacheEvict(allEntries = true)
  fun upsertLocationProduct(locationProductEntity: LocationProductEntity) {
    locationProductRepository.save(locationProductEntity)
  }
}
