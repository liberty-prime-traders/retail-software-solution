package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationDto
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationInsertDto
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION])
class OrganizationCache(
    private val organizationRepository: OrganizationRepository,
    private val mapper: OrganizationMapper
) {

    @Cacheable
    fun getAllOrganizations(): Collection<OrganizationDto> = organizationRepository.findAll().map { mapper.toDomainDto(it) }

    @Cacheable
    fun getOrganizationByDomain(domain: String): OrganizationDto? =
        organizationRepository.findOneBySubdomain(domain)?.let { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: OrganizationInsertDto, schemaName: String, passId: UUID): OrganizationDto {
        val entity = mapper.toEntity(insertDto).apply {
            this.schemaName = schemaName
            this.creationPassId = passId
            this.subdomain = insertDto.subdomain
        }
        val saved = organizationRepository.saveAndFlush(entity)
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: OrganizationDto): OrganizationDto {
        val saved = organizationRepository.save(mapper.toEntity(dto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun deleteOrganization(id: UUID) {
        organizationRepository.deleteById(id)
    }
}
