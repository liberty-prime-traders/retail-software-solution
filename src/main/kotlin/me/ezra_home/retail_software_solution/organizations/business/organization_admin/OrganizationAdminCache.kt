package me.ezra_home.retail_software_solution.organizations.business.organization_admin

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.OrganizationAdminEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_ADMIN])
internal class OrganizationAdminCache(private val organizationAdminRepository: OrganizationAdminRepository) {

    @Cacheable
    fun getAdminHistory(): Collection<OrganizationAdminEntity> {
        return organizationAdminRepository.findAll()
    }

    @CacheEvict(allEntries = true)
    fun upsertOrganizationAdmin(organizationAdminEntity: OrganizationAdminEntity) {
        organizationAdminRepository.save(organizationAdminEntity)
    }
}
