package me.ezra_home.retail_software_solution.platform.business.organization.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationResponseDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgProfileService
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
    private val authorizationPassService: AuthorizationPassService,
    private val orgProfileService: OrgProfileService
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllOrganizations(): Collection<OrganizationResponseDto> =
        organizationCache.getAllOrganizations().map { organizationMapper.toResponseDto(it) }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllOrganizationDtos(): Collection<OrganizationDto> = organizationCache.getAllOrganizations()

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getBySchema(schema: String): OrganizationDto =
        organizationCache.getAllOrganizations().find { it.schemaName == schema }
            ?: throw RtsGenericException("No organization found for schema $schema.")

    fun updateCurrentDbVersion(organizationId: UUID, versionId: UUID) {
        val org = organizationCache.getAllOrganizations().find { it.id == organizationId }
            ?: throw RtsGenericException("Organization not found")
        organizationCache.save(org.copy(currentDbVersionId = versionId))
    }

    fun createOrganization(dto: OrganizationInsertDto): OrganizationResponseDto {
        val passId = authorizationPassService.redeem(dto.passCode, PassType.CREATE_ORGANIZATION)
        organizationValidator.validateNameOnSave(dto.name)
        DateTimes.validateTimezone(dto.timezone)
        markSubdomainAsUsed(dto.subdomain)
        val schemaName = createOrganizationSchema(dto.subdomain)
        try {
            val saved = organizationCache.create(dto, schemaName, passId)
            SessionContextProvider.initOrganization(saved)
            organizationAdminService.registerFounder(saved.createdById)
            organizationUserService.registerFounder(saved.createdById)
            orgProfileService.applySeedDefaults()
            return organizationMapper.toResponseDto(saved)
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
        val existing = organizationCache.getAllOrganizations()
            .find { it.id == organizationId } ?: throw NotFoundException()
        val saved = organizationCache.save(dto.applyTo(existing))
        return organizationMapper.toResponseDto(saved)
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

    fun getAllOrganizationsWithLocations(): Collection<OrganizationWithLocations> =
        organizationCache.getAllOrganizations().map { organization ->
            SessionContextProvider.initOrganization(organization)
            OrganizationWithLocations(organization = organization, locations = locationService.getAllLocations())
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
