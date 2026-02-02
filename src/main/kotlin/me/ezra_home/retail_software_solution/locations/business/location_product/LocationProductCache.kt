package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.locations.model.LocationProductEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheSchemaLevel(SchemaLevel.LOCATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION_PRODUCT])
class LocationProductCache(private val locationProductRepository: LocationProductRepository) {

  @Cacheable
  fun findAllLocationProducts(): List<LocationProductEntity> = locationProductRepository.findAllLocationProducts()

  @Cacheable
  fun countAllLocationProducts(): Long = locationProductRepository.count()

  @CacheEvict(allEntries = true)
  fun upsertLocationProduct(locationProductEntity: LocationProductEntity) {
    locationProductRepository.save(locationProductEntity)
  }
}
