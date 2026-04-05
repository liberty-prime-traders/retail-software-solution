package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION])
class LocationCache(
    private val locationRepository: LocationRepository,
    private val locationMapper: LocationMapper
) {

    @Cacheable
    fun getAllLocations(): Collection<LocationDto> {
        return locationRepository.findAll().map { locationMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun upsertLocation(locationDto: LocationDto) {
        locationRepository.save(locationMapper.toEntity(locationDto))
    }
}
