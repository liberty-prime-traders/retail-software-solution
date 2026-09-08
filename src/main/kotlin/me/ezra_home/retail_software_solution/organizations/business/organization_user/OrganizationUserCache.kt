package me.ezra_home.retail_software_solution.organizations.business.organization_user

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.organization_user.api.OrganizationUserInsertDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_USER])
class OrganizationUserCache(
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
    fun create(insertDto: OrganizationUserInsertDto): OrganizationUserDto {
        val saved = organizationUserRepository.save(organizationUserMapper.toEntity(insertDto))
        return organizationUserMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun createAll(insertDtos: Collection<OrganizationUserInsertDto>): List<OrganizationUserDto> {
        val entities = insertDtos.map { organizationUserMapper.toEntity(it) }
        return organizationUserRepository.saveAllAndFlush(entities).map { organizationUserMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun save(dto: OrganizationUserDto): OrganizationUserDto {
        val saved = organizationUserRepository.save(organizationUserMapper.toEntity(dto))
        return organizationUserMapper.toDomainDto(saved)
    }
}
