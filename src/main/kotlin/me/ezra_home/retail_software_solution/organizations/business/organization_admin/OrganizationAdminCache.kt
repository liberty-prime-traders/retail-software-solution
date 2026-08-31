package me.ezra_home.retail_software_solution.organizations.business.organization_admin

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.api.OrganizationAdminInsertDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_ADMIN])
class OrganizationAdminCache(
    private val organizationAdminRepository: OrganizationAdminRepository,
    private val organizationAdminMapper: OrganizationAdminMapper
) {

    @Cacheable
    fun getAdminHistory(): Collection<OrganizationAdminDto> {
        return organizationAdminRepository.findAll().map { organizationAdminMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun create(insertDto: OrganizationAdminInsertDto): OrganizationAdminDto {
        val saved = organizationAdminRepository.save(organizationAdminMapper.toEntity(insertDto))
        return organizationAdminMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: OrganizationAdminDto): OrganizationAdminDto {
        val saved = organizationAdminRepository.save(organizationAdminMapper.toEntity(dto))
        return organizationAdminMapper.toDomainDto(saved)
    }
}
