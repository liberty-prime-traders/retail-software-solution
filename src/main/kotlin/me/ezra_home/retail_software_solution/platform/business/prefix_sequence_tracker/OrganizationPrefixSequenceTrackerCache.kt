package me.ezra_home.retail_software_solution.platform.business.prefix_sequence_tracker

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PREFIX_SEQUENCE_TRACKER])
class OrganizationPrefixSequenceTrackerCache(
    private val prefixSequenceTrackerRepository: OrganizationPrefixSequenceTrackerRepository
) {
    @Cacheable
    fun findByTableRegistryIdAndPrefix(tableRegistryId: UUID, prefix: String): OrganizationPrefixSequenceTrackerEntity? {
        return prefixSequenceTrackerRepository.findByTableRegistryIdAndPrefix(tableRegistryId, prefix)
    }

    @CacheEvict(allEntries = true)
    fun save(entity: OrganizationPrefixSequenceTrackerEntity): OrganizationPrefixSequenceTrackerEntity {
        return prefixSequenceTrackerRepository.save(entity)
    }
}
