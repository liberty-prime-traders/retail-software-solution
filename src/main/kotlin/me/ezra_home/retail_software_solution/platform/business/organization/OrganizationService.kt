package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationUpsertDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.OrganizationJoinRequestMapper
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.OrganizationJoinRequestService
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationAdminJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationJoinRequestResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationLaunchResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization_user.OrganizationUserService
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.OrganizationAdminCache
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.OrganizationAdminService
import me.ezra_home.retail_software_solution.platform.business.subdomain.ReservedSubdomainService
import me.ezra_home.retail_software_solution.platform.model.OrganizationAdminEntity
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import me.ezra_home.retail_software_solution.platform.model.OrganizationJoinRequestEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.enums.JoinRequestStatus
import me.ezra_home.retail_software_solution.util.enums.Status
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class OrganizationService(
    private val organizationMapper: OrganizationMapper,
    private val organizationJoinRequestMapper: OrganizationJoinRequestMapper,
    private val organizationCache: OrganizationCache,
    private val reservedSubdomainService: ReservedSubdomainService,
    private val organizationAdminCache: OrganizationAdminCache,
    private val organizationValidator: OrganizationValidator,
    private val organizationUserService: OrganizationUserService,
    private val organizationJoinRequestService: OrganizationJoinRequestService,
    private val organizationAdminService: OrganizationAdminService
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllOrganizations(): Collection<OrganizationResponseDto> {
        return organizationCache.getAllOrganizations().map { organizationMapper.toResponseDto(it) }
    }

    fun createOrganization(organizationInsertDto: OrganizationUpsertDto): OrganizationResponseDto {
        organizationValidator.validateNameOnSave(organizationInsertDto.name)
        markSubdomainAsUsed(organizationInsertDto)
        val entity = organizationMapper.toEntity(organizationInsertDto)
        organizationCache.upsertOrganization(entity)
        organizationAdminCache.upsertOrganization(OrganizationAdminEntity(entity.id).apply {
            adminId = entity.createdById
        })
        return organizationMapper.toResponseDto(entity)
    }

    private fun markSubdomainAsUsed(organizationInsertDto: OrganizationUpsertDto) {
        val intendedSubdomain = organizationInsertDto.subdomain
        if (intendedSubdomain.isNullOrBlank()) {
            throw RtsGenericException("An Organization must have a subdomain")
        }
        val reservedSubdomain = reservedSubdomainService.getReservedSubdomains()
            .find { it.subdomain == intendedSubdomain && it.status == Status.UNUSED }
        if (reservedSubdomain == null) {
            throw RtsGenericException("Subdomain '$intendedSubdomain' was not reserved")
        }
        reservedSubdomainService.markSubdomainAsUsed(reservedSubdomain.id)
    }

    fun updateOrganization(organizationUpdateDto: OrganizationUpsertDto): OrganizationResponseDto {
        val organizationId = SessionContextProvider.getOrganizationId()
        organizationValidator.validateNameOnSave(organizationUpdateDto.name, organizationId)
        val entityFromDatabase =
            organizationCache.getAllOrganizations().find { it.id == organizationId } ?: throw NotFoundException()
        organizationMapper.partialUpdate(organizationUpdateDto, entityFromDatabase)
        organizationCache.upsertOrganization(entityFromDatabase)
        return organizationMapper.toResponseDto(entityFromDatabase)
    }

    fun deleteOrganization() {
        val organizationId = SessionContextProvider.getOrganizationId()
        organizationCache.getAllOrganizations().find { it.id == organizationId }?.let { entity ->
            val usageCount = entity.usageCount
            if (usageCount > 0L) {
                throw RtsGenericException("Organization ${entity.name} has $usageCount usage(s) and cannot be deleted")
            }
            organizationCache.deleteOrganization(organizationId)
        }
    }

    fun attemptOrganizationLaunch(domain: String): OrganizationLaunchResponseDto {
        val sysUserId = SessionContextProvider.getUserId()
        val organization = organizationCache.getOrganizationByDomain(domain)
            ?: return createJoinRequest(domain, sysUserId, null)
        val organizationUserExists =
            organizationUserService.existsByOrganizationIdAndUserId(organization.id!!, sysUserId)
        return if (organizationUserExists) {
            val isOrganizationAdmin = organizationAdminService.isUserAdmin(organization.id!!, sysUserId)
            organizationJoinRequestMapper.toLaunchResponse(
                organization = organizationMapper.toResponseDto(organization),
                isOrganizationAdmin = isOrganizationAdmin,
                accessRequested = false
            )
        } else {
            createJoinRequest(domain, sysUserId, organization)
        }
    }

    private fun createJoinRequest(
        domain: String,
        userId: UUID,
        organization: OrganizationEntity?
    ): OrganizationLaunchResponseDto {
        val hasPendingRequest = organizationJoinRequestService.existsBySubdomainAndCreatedByIdAndStatus(
            domain, userId, JoinRequestStatus.PENDING
        )
        if (hasPendingRequest.not()) {
            organizationJoinRequestService.createOrganizationJoinRequest(
                OrganizationJoinRequestEntity(
                    subdomain = domain,
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

    fun getUserJoinRequests(): Collection<OrganizationJoinRequestResponseDto> {
        val sysUserId = SessionContextProvider.getUserId()
        return organizationJoinRequestService.getUserJoinRequests(sysUserId)
    }

    fun getOrganizationJoinRequests(): Collection<OrganizationAdminJoinRequestResponseDto> {
        val organizationId = SessionContextProvider.getOrganizationId()
        return organizationJoinRequestService.getOrganizationJoinRequests(organizationId)
    }

}
