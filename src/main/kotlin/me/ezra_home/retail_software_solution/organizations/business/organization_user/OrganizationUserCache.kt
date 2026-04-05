package me.ezra_home.retail_software_solution.organizations.business.organization_user

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_USER])
internal class OrganizationUserCache(
    private val organizationUserRepository: OrganizationUserRepository,
    private val organizationUserMapper: OrganizationUserMapper
) {
    @Cacheable
    fun getOrganizationUsers(): Collection<OrganizationUserDto> {
        return organizationUserRepository.findAll().map { organizationUserMapper.toDomainDto(it) }
    }

    @Cacheable
    fun existsByOrganizationIdAndUserId(userId: UUID): Boolean {
        return organizationUserRepository.existsByUserId(userId)
    }

    @CacheEvict(allEntries = true)
    fun upsertOrganizationUser(dto: OrganizationUserDto) {
        organizationUserRepository.save(organizationUserMapper.toEntity(dto))
    }

    @CacheEvict(allEntries = true)
    fun upsertOrganizationUsers(dtos: Collection<OrganizationUserDto>) {
        organizationUserRepository.saveAll(dtos.map { organizationUserMapper.toEntity(it) })
    }
}
