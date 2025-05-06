package me.ezra_home.retail_software_solution.platform.business.organizationadmin

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.model.OrganizationAdminEntity
import me.ezra_home.retail_software_solution.util.exceptions.QueriedByEmptyIdException
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_ADMIN])
class OrganizationAdminCache(private val organizationAdminRepository: OrganizationAdminRepository) {

    @Cacheable
    fun getAdminHistoryForOrganization(organizationId: UUID?): Collection<OrganizationAdminEntity> {
        return organizationId?.let { organizationAdminRepository.findByOrganizationId(it) }
            ?: throw QueriedByEmptyIdException()
    }

    @CacheEvict(allEntries = true)
    fun upsertOrganizationAdmin(organizationAdminEntity: OrganizationAdminEntity) {
        organizationAdminRepository.save(organizationAdminEntity)
    }
}
