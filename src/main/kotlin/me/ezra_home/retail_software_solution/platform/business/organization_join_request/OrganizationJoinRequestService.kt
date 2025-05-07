package me.ezra_home.retail_software_solution.platform.business.organization_join_request

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationMapper
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationAdminJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationLaunchResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_user.OrganizationUserService
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.OrganizationAdminService
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import me.ezra_home.retail_software_solution.platform.model.OrganizationJoinRequestEntity
import me.ezra_home.retail_software_solution.util.enums.JoinRequestStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class OrganizationJoinRequestService(
    private val organizationMapper: OrganizationMapper,
    private val organizationJoinRequestCache: OrganizationJoinRequestCache,
    private val organizationJoinRequestMapper: OrganizationJoinRequestMapper,
    private val organizationCache: OrganizationCache,
    private val organizationUserService: OrganizationUserService,
    private val organizationAdminService: OrganizationAdminService
) {

    fun attemptOrganizationLaunch(domain: String, userId: UUID): OrganizationLaunchResponseDto {
        val organization = organizationCache.getOrganizationByDomain(domain)
            ?: return createJoinRequest(domain, userId, null)
        return if (isOrganizationMember(organization.id!!, userId)) {
            val isOrganizationAdmin = organizationAdminService.isOrganizationAdmin(organization.id!!, userId)
            organizationJoinRequestMapper.toLaunchResponse(
                organization = organizationMapper.toResponseDto(organization),
                isOrganizationAdmin = isOrganizationAdmin,
                accessRequested = false
            )
        } else {
            createJoinRequest(domain, userId, organization)
        }
    }

    private fun isOrganizationMember(organizationId: UUID, userId: UUID): Boolean {
        return organizationUserService.existsByOrganizationIdAndUserId(organizationId, userId)
    }

    private fun createJoinRequest(
        subdomain: String,
        userId: UUID,
        organization: OrganizationEntity?
    ): OrganizationLaunchResponseDto {
        val hasPendingRequest = existsBySubdomainAndCreatedByIdAndStatus(
            subdomain, userId, JoinRequestStatus.PENDING
        )
        if (hasPendingRequest.not()) {
            createOrganizationJoinRequest(
                OrganizationJoinRequestEntity(
                    subdomain = subdomain,
                    status = JoinRequestStatus.PENDING,
                    organizationId = organization?.id
                ).apply { createdById = userId }
            )
        }
        return organizationJoinRequestMapper.toLaunchResponse(
            organization = organization?.let { organizationMapper.toResponseDto(it) },
            isOrganizationAdmin = false,
            accessRequested = true,
        )
    }

    fun createOrganizationJoinRequest(organizationJoinRequestEntity: OrganizationJoinRequestEntity) {
        organizationJoinRequestCache.upsertOrganizationJoinRequest(organizationJoinRequestEntity)
    }

    fun existsBySubdomainAndCreatedByIdAndStatus(
        subdomain: String,
        userId: UUID,
        status: JoinRequestStatus
    ): Boolean = organizationJoinRequestCache.existsBySubdomainAndCreatedByIdAndStatus(subdomain, userId, status)

    fun getUserJoinRequests(userId: UUID): Collection<OrganizationJoinRequestResponseDto> {
        return organizationJoinRequestCache.getUserJoinRequests(userId)
            .map { organizationJoinRequestMapper.toDto(it) }
    }

    fun getOrganizationJoinRequests(organizationId: UUID): Collection<OrganizationAdminJoinRequestResponseDto> {
        return organizationJoinRequestCache.getOrganizationJoinRequests(organizationId)
            .map { organizationJoinRequestMapper.toAdminDto(it) }
    }

}
