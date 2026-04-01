package me.ezra_home.retail_software_solution.platform.business.organization_join_request

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.model.OrganizationJoinRequestEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.ORGANIZATION_JOIN_REQUEST])
class OrganizationJoinRequestCache(private val organizationJoinRequestRepository: OrganizationJoinRequestRepository) {

    @CacheEvict(allEntries = true)
    fun upsertOrganizationJoinRequest(organizationJoinRequestEntity: OrganizationJoinRequestEntity) {
        organizationJoinRequestRepository.save(organizationJoinRequestEntity)
    }

    @CacheEvict(allEntries = true)
    fun upsertOrganizationJoinRequests(organizationJoinRequestEntities: Collection<OrganizationJoinRequestEntity>) {
        organizationJoinRequestRepository.saveAll(organizationJoinRequestEntities)
    }

    @Cacheable
    fun existsBySubdomainAndCreatedByIdAndStatus(
        subdomain: String,
        userId: UUID,
        status: JoinRequestStatus
    ) = organizationJoinRequestRepository.existsBySubdomainAndCreatedByIdAndStatus(subdomain, userId, status)

    @Cacheable
    fun getUserJoinRequests(userId: UUID): Collection<OrganizationJoinRequestEntity> {
        return organizationJoinRequestRepository.findAllByCreatedById(userId)
    }

    @Cacheable
    fun getOrganizationJoinRequests(organizationId: UUID): Collection<OrganizationJoinRequestEntity> {
        return organizationJoinRequestRepository.findByOrganizationId(organizationId)
    }

}
