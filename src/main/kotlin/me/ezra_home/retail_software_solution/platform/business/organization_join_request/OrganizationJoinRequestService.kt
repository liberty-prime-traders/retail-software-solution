package me.ezra_home.retail_software_solution.platform.business.organization_join_request

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.organization_user.OrganizationUserService
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationAdminJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationLaunchResponseDto
import org.springframework.stereotype.Service
import java.util.Collections
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class OrganizationJoinRequestService(
    private val organizationJoinRequestCache: OrganizationJoinRequestCache,
    private val organizationJoinRequestMapper: OrganizationJoinRequestMapper,
    private val organizationUserService: OrganizationUserService
) {

    fun createJoinRequest(
        subdomain: String, userId: UUID, organization: OrganizationDto?
    ): OrganizationLaunchResponseDto {
        val hasPendingRequest = organizationJoinRequestCache.existsBySubdomainAndCreatedByIdAndStatus(
            subdomain, userId, JoinRequestStatus.PENDING
        )
        if (hasPendingRequest.not()) {
            val joinRequestDto = OrganizationJoinRequestDto(
                subdomain = subdomain,
                status = JoinRequestStatus.PENDING,
                organizationId = organization?.id
            )
            organizationJoinRequestCache.upsertOrganizationJoinRequest(joinRequestDto)
        }
        return organizationJoinRequestMapper.toLaunchResponse(
            organization = null,
            isOrganizationAdmin = false,
            accessRequested = true
        )
    }

    fun getUserJoinRequests(): Collection<OrganizationJoinRequestResponseDto> {
        return organizationJoinRequestCache.getUserJoinRequests(SessionContextProvider.getUserId())
            .map { organizationJoinRequestMapper.toDto(it) }
    }

    fun getOrganizationJoinRequests(): Collection<OrganizationAdminJoinRequestResponseDto> {
        return organizationJoinRequestCache.getOrganizationJoinRequests(SessionContextProvider.getOrganizationId())
            .map { organizationJoinRequestMapper.toAdminDto(it) }
    }

    fun admitUsers(joinRequestIds: Collection<UUID>): Collection<OrganizationAdminJoinRequestResponseDto> {
        val joinRequestsResponse = updateJoinRequests(joinRequestIds, JoinRequestStatus.APPROVED)
        organizationUserService.admitJoinRequests(joinRequestsResponse)
        return joinRequestsResponse
    }

    fun denyUsers(joinRequestIds: Collection<UUID>): Collection<OrganizationAdminJoinRequestResponseDto> {
        return updateJoinRequests(joinRequestIds, JoinRequestStatus.DENIED)
    }

    private fun updateJoinRequests(
        joinRequestIds: Collection<UUID>,
        newStatus: JoinRequestStatus
    ): Collection<OrganizationAdminJoinRequestResponseDto> {
        if (joinRequestIds.isEmpty()) {
            return Collections.emptyList()
        }
        return organizationJoinRequestCache.getOrganizationJoinRequests(SessionContextProvider.getOrganizationId())
            .filter { joinRequestIds.contains(it.id) && it.status == JoinRequestStatus.PENDING }
            .map { it.status = newStatus; it }
            .let { filteredJoinRequests ->
                organizationJoinRequestCache.upsertOrganizationJoinRequests(filteredJoinRequests)
                filteredJoinRequests.map { organizationJoinRequestMapper.toAdminDto(it) }
            }
    }
}
