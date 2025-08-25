package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION])
class LocationCache(private val locationRepository: LocationRepository) {

    @Cacheable
    fun getAllLocations(): Collection<LocationEntity> {
        return locationRepository.findAll()
    }

    @CacheEvict(allEntries = true)
    fun upsertLocation(locationEntity: LocationEntity) {
        locationRepository.save(locationEntity)
    }

    @CacheEvict(allEntries = true)
    fun deleteLocation(id: UUID?) {
        if (id != null) {
            locationRepository.deleteById(id)
        }
    }
}
