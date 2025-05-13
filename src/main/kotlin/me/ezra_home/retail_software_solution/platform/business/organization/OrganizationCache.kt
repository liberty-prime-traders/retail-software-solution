package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION])
class OrganizationCache(private val organizationRepository: OrganizationRepository) {

    @Cacheable
    fun getAllOrganizations(): Collection<OrganizationEntity> {
        return organizationRepository.findAll()
    }

    @Cacheable
    fun getOrganizationByDomain(domain: String): OrganizationEntity? {
        return organizationRepository.findOneBySubdomain(domain)
    }

    @CacheEvict(allEntries = true)
    fun upsertOrganization(organizationEntity: OrganizationEntity) {
        organizationRepository.save(organizationEntity)
    }

    @CacheEvict(allEntries = true)
    fun deleteOrganization(id: UUID) {
        organizationRepository.deleteById(id)
    }
}
