package me.ezra_home.retail_software_solution.organizations.business.prefix_configuration

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.organizations.model.OrganizationPrefixConfigurationEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_PREFIX_CONFIGURATION])
class OrganizationPrefixConfigurationCache(
    private val organizationPrefixConfigurationRepository: OrganizationPrefixConfigurationRepository
) {
    @Cacheable
    fun getById(id: UUID): OrganizationPrefixConfigurationEntity? =
        organizationPrefixConfigurationRepository.findById(id).orElse(null)

    @Cacheable
    fun getForTableRegistry(tableRegistryId: UUID): Collection<OrganizationPrefixConfigurationEntity> =
        organizationPrefixConfigurationRepository.findByTableRegistryId(tableRegistryId)

    @CacheEvict(allEntries = true)
    fun upsertPrefixConfiguration(entity: OrganizationPrefixConfigurationEntity) =
        organizationPrefixConfigurationRepository.save(entity)
}
