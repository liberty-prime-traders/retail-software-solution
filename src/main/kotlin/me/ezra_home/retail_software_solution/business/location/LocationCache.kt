package me.ezra_home.retail_software_solution.business.location

import me.ezra_home.retail_software_solution.business.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.model.entity.LocationEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.LOCATION])
class LocationCache(private val locationRepository: LocationRepository) {

    @Cacheable
    fun getByOrganizationId(organizationId: UUID?): Collection<LocationEntity> {
        if (organizationId == null) {
            throw QueriedByEmptyIdException()
        }
        return locationRepository.findByOrganizationId(organizationId)
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
