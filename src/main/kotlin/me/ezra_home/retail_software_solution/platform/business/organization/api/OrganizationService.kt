package me.ezra_home.retail_software_solution.platform.business.organization.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationResponseDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.api.OrganizationAdminService
import me.ezra_home.retail_software_solution.organizations.business.organization_user.api.OrganizationUserService
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.api.AuthorizationPassService
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.api.PassType
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationMapper
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationSchemaService
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationValidator
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.OrganizationJoinRequestService
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.OrganizationLaunchResponseDto
import me.ezra_home.retail_software_solution.platform.business.reserved_subdomain.api.ReservedDomainStatus
import me.ezra_home.retail_software_solution.platform.business.reserved_subdomain.api.ReservedSubdomainService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.SchemaNameGenerator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class OrganizationService(
    private val organizationMapper: OrganizationMapper,
    private val organizationCache: OrganizationCache,
    private val locationService: LocationService,
    private val reservedSubdomainService: ReservedSubdomainService,
    private val organizationAdminService: OrganizationAdminService,
    private val organizationValidator: OrganizationValidator,
    private val organizationJoinRequestService: OrganizationJoinRequestService,
    private val organizationSchemaService: OrganizationSchemaService,
    private val organizationUserService: OrganizationUserService,
    private val authorizationPassService: AuthorizationPassService
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllOrganizations(): Collection<OrganizationResponseDto> {
        return organizationCache.getAllOrganizations().map { organizationMapper.toResponseDto(it) }
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllOrganizationDtos(): Collection<OrganizationDto> = organizationCache.getAllOrganizations()

    fun updateCurrentDbVersion(organizationId: UUID, versionId: UUID) {
        val org = organizationCache.getAllOrganizations().find { it.id == organizationId }
            ?: throw RtsGenericException("Organization not found")
        org.currentDbVersionId = versionId
        organizationCache.upsertOrganization(org)
    }

    fun createOrganization(dto: OrganizationInsertDto): OrganizationResponseDto {
        val pass = authorizationPassService.redeem(dto.passCode, PassType.CREATE_ORGANIZATION)
        organizationValidator.validateNameOnSave(dto.name)
        DateTimes.validateTimezone(dto.timezone)
        markSubdomainAsUsed(dto.subdomain)
        val schemaName = createOrganizationSchema(dto.subdomain)
        try {
            val organizationDto = organizationMapper.toDomainDto(dto).apply {
                this.schemaName = schemaName
                this.creationPassId = pass.id
            }
            organizationCache.upsertOrganization(organizationDto)
            SessionContextProvider.initOrganization(organizationDto)
            organizationAdminService.registerFounder(organizationDto.createdById!!)
            organizationUserService.registerFounder(organizationDto.createdById!!)
            return organizationMapper.toResponseDto(organizationDto)
        } catch (e: Exception) {
            organizationSchemaService.dropSchema(schemaName)
            throw e
        }
    }

    private fun createOrganizationSchema(subdomain: String): String {
        val schemaName = SchemaNameGenerator.convertTrustedSubdomainToSchema(subdomain, "org")
        organizationSchemaService.createSchema(schemaName)
        return schemaName
    }

    private fun markSubdomainAsUsed(subdomain: String) {
        val reservedSubdomain = reservedSubdomainService.getReservedSubdomains()
            .find { it.subdomain == subdomain && it.status == ReservedDomainStatus.UNUSED }
            ?: throw RtsGenericException("Subdomain '$subdomain' was not reserved")
        reservedSubdomainService.markSubdomainAsUsed(reservedSubdomain.id)
    }

    fun updateOrganization(dto: OrganizationUpdateDto): OrganizationResponseDto {
        val organizationId = SessionContextProvider.getOrganizationId()
        organizationValidator.validateNameOnSave(dto.name, organizationId)
        DateTimes.validateTimezone(dto.timezone)
        val organizationDto = organizationCache.getAllOrganizations()
            .find { it.id == organizationId } ?: throw NotFoundException()
        organizationMapper.partialUpdate(dto, organizationDto)
        organizationCache.upsertOrganization(organizationDto)
        return organizationMapper.toResponseDto(organizationDto)
    }

    fun deleteOrganization() {
        val organizationId = SessionContextProvider.getOrganizationId()
        organizationCache.getAllOrganizations().find { it.id == organizationId }?.apply {
            organizationCache.deleteOrganization(organizationId)
        }
    }

    fun attemptOrganizationLaunch(domain: String): OrganizationLaunchResponseDto {
        val userId = SessionContextProvider.getUserId()
        val organization = organizationCache.getOrganizationByDomain(domain)
            ?: return organizationJoinRequestService.createJoinRequest(domain, userId, null)
        SessionContextProvider.initOrganization(organization)
        return if (organizationUserService.isOrganizationMember(userId)) {
            organizationJoinRequestService.buildMemberLaunchResponse(
                organization = organizationMapper.toResponseDto(organization),
                isOrganizationAdmin = organizationAdminService.isOrganizationAdmin()
            )
        } else {
            organizationJoinRequestService.createJoinRequest(domain, userId, organization)
        }
    }

    fun getAllOrganizationsWithLocations(): Collection<OrganizationWithLocations> {
        return organizationCache.getAllOrganizations().map { organization ->
            SessionContextProvider.initOrganization(organization)
            OrganizationWithLocations(organization = organization, locations = locationService.getAllLocations())
        }
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getOrganizationLocations(organizationId: UUID): Collection<LocationResponseDto> {
        val organization = organizationCache.getAllOrganizations()
            .find { it.id == organizationId }
            ?: throw RtsGenericException("The organization with id $organizationId does not exist")
        SessionContextProvider.initOrganization(organization)
        return locationService.getAllLocations()
    }
}
