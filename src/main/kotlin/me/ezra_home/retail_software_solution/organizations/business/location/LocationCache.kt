package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION])
internal class LocationCache(private val locationRepository: LocationRepository) {

    @Cacheable
    fun getAllLocations(): Collection<LocationEntity> {
        return locationRepository.findAll()
    }

    @CacheEvict(allEntries = true)
    fun upsertLocation(locationEntity: LocationEntity) {
        locationRepository.save(locationEntity)
    }

}
