package me.ezra_home.retail_software_solution.platform.business.organization_join_request

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.JoinRequestStatus
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.OrganizationJoinRequestInsertDto
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_JOIN_REQUEST])
class OrganizationJoinRequestCache(
    private val organizationJoinRequestRepository: OrganizationJoinRequestRepository,
    private val mapper: OrganizationJoinRequestMapper
) {

    @CacheEvict(allEntries = true)
    fun create(insertDto: OrganizationJoinRequestInsertDto): OrganizationJoinRequestDto {
        val saved = organizationJoinRequestRepository.saveAndFlush(mapper.toEntity(insertDto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun saveAll(dtos: Collection<OrganizationJoinRequestDto>): List<OrganizationJoinRequestDto> {
        val saved = organizationJoinRequestRepository.saveAll(dtos.map { mapper.toEntity(it) })
        return saved.map { mapper.toDomainDto(it) }
    }

    @Cacheable
    fun existsBySubdomainAndCreatedByIdAndStatus(
        subdomain: String,
        userId: UUID,
        status: JoinRequestStatus
    ) = organizationJoinRequestRepository.existsBySubdomainAndCreatedByIdAndStatus(subdomain, userId, status)

    @Cacheable
    fun getUserJoinRequests(userId: UUID): Collection<OrganizationJoinRequestDto> =
        organizationJoinRequestRepository.findAllByCreatedById(userId).map { mapper.toDomainDto(it) }

    @Cacheable
    fun getOrganizationJoinRequests(organizationId: UUID): Collection<OrganizationJoinRequestDto> =
        organizationJoinRequestRepository.findByOrganizationId(organizationId).map { mapper.toDomainDto(it) }
}
