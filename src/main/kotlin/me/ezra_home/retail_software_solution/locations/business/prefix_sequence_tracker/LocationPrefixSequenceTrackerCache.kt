package me.ezra_home.retail_software_solution.locations.business.prefix_sequence_tracker

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.rest.endpoints.prefix_sequence_tracker.LocationPrefixSequenceTrackerEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.LOCATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION_PREFIX_SEQUENCE_TRACKER])
@TransactionalOnLocationSchema
class LocationPrefixSequenceTrackerCache(
    private val locationPrefixSequenceTrackerRepository: LocationPrefixSequenceTrackerRepository
) {
    @Cacheable
    @TransactionalOnLocationSchema(readOnly = true)
    fun findByTableRegistryIdAndPrefix(tableRegistryId: UUID, prefix: String): LocationPrefixSequenceTrackerEntity? {
        return locationPrefixSequenceTrackerRepository.findByTableRegistryIdAndPrefix(tableRegistryId, prefix)
    }

    @CacheEvict(allEntries = true)
    fun save(entity: LocationPrefixSequenceTrackerEntity): LocationPrefixSequenceTrackerEntity {
        return locationPrefixSequenceTrackerRepository.save(entity)
    }
}
