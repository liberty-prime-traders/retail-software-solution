package me.ezra_home.retail_software_solution.organizations.business.organization_user

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.organizations.model.OrganizationUserEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_USER])
class OrganizationUserCache(private val organizationUserRepository: OrganizationUserRepository) {

    @Cacheable
    fun existsByOrganizationIdAndUserId(userId: UUID): Boolean {
        return organizationUserRepository.existsByUserId(userId)
    }

    @CacheEvict(allEntries = true)
    fun upsertOrganizationUser(organizationUserEntity: OrganizationUserEntity) {
        organizationUserRepository.save(organizationUserEntity)
    }
}
