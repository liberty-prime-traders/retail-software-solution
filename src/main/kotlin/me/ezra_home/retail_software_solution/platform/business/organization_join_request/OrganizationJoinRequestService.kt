package me.ezra_home.retail_software_solution.platform.business.organization_join_request

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationAdminJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.model.OrganizationJoinRequestEntity
import me.ezra_home.retail_software_solution.util.enums.JoinRequestStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class OrganizationJoinRequestService(
    private val organizationJoinRequestCache: OrganizationJoinRequestCache,
    private val organizationJoinRequestMapper: OrganizationJoinRequestMapper
) {

    fun createOrganizationJoinRequest(organizationJoinRequestEntity: OrganizationJoinRequestEntity) {
        organizationJoinRequestCache.upsertOrganizationJoinRequest(organizationJoinRequestEntity)
    }

    fun existsBySubdomainAndCreatedByIdAndStatus(
        subdomain: String,
        userId: UUID,
        status: JoinRequestStatus
    ): Boolean = organizationJoinRequestCache.existsBySubdomainAndCreatedByIdAndStatus(subdomain, userId, status)

    fun getUserJoinRequests(userId: UUID): Collection<OrganizationJoinRequestResponseDto> {
        return organizationJoinRequestCache.getLoggedInUserJoinRequest(userId)
            .map { organizationJoinRequestMapper.toDto(it) }
    }

    fun getOrganizationJoinRequests(organizationId: UUID): Collection<OrganizationAdminJoinRequestResponseDto> {
        return organizationJoinRequestCache.getOrganizationJoinRequests(organizationId)
            .map { organizationJoinRequestMapper.toAdminDto(it) }
    }

}
