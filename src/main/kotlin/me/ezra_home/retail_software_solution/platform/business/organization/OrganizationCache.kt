package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationDto
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION])
internal class OrganizationCache(
    private val organizationRepository: OrganizationRepository,
    private val mapper: OrganizationMapper
) {

  @Cacheable
  fun getAllOrganizations(): Collection<OrganizationDto> {
    return organizationRepository.findAll().map { mapper.toDomainDto(it) }
  }

  @Cacheable
  fun getOrganizationByDomain(domain: String): OrganizationDto? {
    return organizationRepository.findOneBySubdomain(domain)?.let { mapper.toDomainDto(it) }
  }

  @CacheEvict(allEntries = true)
  fun upsertOrganization(dto: OrganizationDto) {
    organizationRepository.save(mapper.toEntity(dto))
  }

  @CacheEvict(allEntries = true)
  fun deleteOrganization(id: UUID) {
    organizationRepository.deleteById(id)
  }
}
