package me.ezra_home.retail_software_solution.organizations.business.organization_user

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.OrganizationUserEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_USER])
class OrganizationUserCache(private val organizationUserRepository: OrganizationUserRepository) {
    @Cacheable
    fun getOrganizationUsers(): Collection<OrganizationUserEntity> {
        return organizationUserRepository.findAll()
    }

    @Cacheable
    fun existsByOrganizationIdAndUserId(userId: UUID): Boolean {
        return organizationUserRepository.existsByUserId(userId)
    }

    @CacheEvict(allEntries = true)
    fun upsertOrganizationUser(organizationUserEntity: OrganizationUserEntity) {
        organizationUserRepository.save(organizationUserEntity)
    }

    @CacheEvict(allEntries = true)
    fun upsertOrganizationUsers(organizationUserEntities: Collection<OrganizationUserEntity>) {
        organizationUserRepository.saveAll(organizationUserEntities)
    }
}
