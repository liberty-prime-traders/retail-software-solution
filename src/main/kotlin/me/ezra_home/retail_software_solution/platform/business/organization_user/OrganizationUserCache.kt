package me.ezra_home.retail_software_solution.platform.business.organization_user

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.model.OrganizationUserEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_USER])
class OrganizationUserCache(private val organizationUserRepository: OrganizationUserRepository) {

    @Cacheable(key = "{#organizationId,#userId}")
    fun existsByOrganizationIdAndUserId(organizationId: UUID, userId: UUID): Boolean {
        return organizationUserRepository.existsByOrganizationIdAndUserId(organizationId, userId)
    }

    @CacheEvict(allEntries = true)
    fun upsertOrganizationUser(organizationUserEntity: OrganizationUserEntity) {
        organizationUserRepository.save(organizationUserEntity)
    }
}
