package me.ezra_home.retail_software_solution.locations.business.prefix_configuration

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.locations.model.LocationPrefixConfigurationEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.LOCATION_PREFIX_CONFIGURATION])
class LocationPrefixConfigurationCache(
    private val locationPrefixConfigurationRepository: LocationPrefixConfigurationRepository
) {
    @Cacheable
    fun getById(id: UUID): LocationPrefixConfigurationEntity? =
        locationPrefixConfigurationRepository.findById(id).orElse(null)

    @Cacheable
    fun getForTableRegistry(tableRegistryId: UUID): Collection<LocationPrefixConfigurationEntity> =
        locationPrefixConfigurationRepository.findByTableRegistryId(tableRegistryId)

    @CacheEvict(allEntries = true)
    fun upsertPrefixConfiguration(entity: LocationPrefixConfigurationEntity) = locationPrefixConfigurationRepository.save(entity)
}
