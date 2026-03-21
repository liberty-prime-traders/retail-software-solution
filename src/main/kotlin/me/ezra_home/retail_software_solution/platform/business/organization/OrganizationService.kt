package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.organizations.business.location.LocationMapper
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationResponseDto
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.OrganizationAdminCache
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.OrganizationAdminService
import me.ezra_home.retail_software_solution.organizations.business.organization_user.OrganizationUserCache
import me.ezra_home.retail_software_solution.organizations.business.organization_user.OrganizationUserService
import me.ezra_home.retail_software_solution.organizations.model.OrganizationAdminEntity
import me.ezra_home.retail_software_solution.organizations.model.OrganizationUserEntity
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationUpdateDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationWithLocations
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.OrganizationJoinRequestMapper
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.OrganizationJoinRequestService
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationLaunchResponseDto
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.AuthorizationPassService
import me.ezra_home.retail_software_solution.platform.business.reserved_subdomain.ReservedSubdomainService
import me.ezra_home.retail_software_solution.util.business.SchemaNameGenerator
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.PassType
import me.ezra_home.retail_software_solution.platform.business.reserved_subdomain.ReservedDomainStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class OrganizationService(
    private val organizationMapper: OrganizationMapper,
    private val organizationCache: OrganizationCache,
    private val locationCache: LocationCache,
    private val reservedSubdomainService: ReservedSubdomainService,
    private val organizationAdminCache: OrganizationAdminCache,
    private val organizationValidator: OrganizationValidator,
    private val organizationJoinRequestService: OrganizationJoinRequestService,
    private val organizationSchemaService: OrganizationSchemaService,
    private val organizationUserService: OrganizationUserService,
    private val organizationJoinRequestMapper: OrganizationJoinRequestMapper,
    private val organizationAdminService: OrganizationAdminService,
    private val organizationUserCache: OrganizationUserCache,
    private val locationMapper: LocationMapper,
    private val authorizationPassService: AuthorizationPassService
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllOrganizations(): Collection<OrganizationResponseDto> {
        return organizationCache.getAllOrganizations().map { organizationMapper.toResponseDto(it) }
    }

    fun createOrganization(dto: OrganizationInsertDto): OrganizationResponseDto {
        val pass = authorizationPassService.redeem(dto.passCode, PassType.CREATE_ORGANIZATION)
        organizationValidator.validateNameOnSave(dto.name)
        markSubdomainAsUsed(dto.subdomain)
        val schemaName = createOrganizationSchema(dto.subdomain)
        try {
            val entity = organizationMapper.toEntity(dto).apply {
                this.schemaName = schemaName
                this.creationPassId = pass.id
            }
            organizationCache.upsertOrganization(entity)
            SessionContextProvider.initOrganization(entity)
            organizationAdminCache.upsertOrganizationAdmin(OrganizationAdminEntity(entity.createdById!!))
            organizationUserCache.upsertOrganizationUser(OrganizationUserEntity().apply { userId = entity.createdById!! })
            return organizationMapper.toResponseDto(entity)
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
        val entity = organizationCache.getAllOrganizations()
            .find { it.id == organizationId } ?: throw NotFoundException()
        organizationMapper.partialUpdate(dto, entity)
        organizationCache.upsertOrganization(entity)
        return organizationMapper.toResponseDto(entity)
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
            organizationJoinRequestMapper.toLaunchResponse(
                organization = organizationMapper.toResponseDto(organization),
                isOrganizationAdmin = organizationAdminService.isOrganizationAdmin(),
                accessRequested = false
            )
        } else {
            organizationJoinRequestService.createJoinRequest(domain, userId, organization)
        }
    }

    fun getAllOrganizationsWithLocations(): Collection<OrganizationWithLocations> {
        val organizationsWithLocations = mutableListOf<OrganizationWithLocations>()
        for (organization in organizationCache.getAllOrganizations()) {
            SessionContextProvider.initOrganization(organization)
            val locations = locationCache.getAllLocations()
            organizationsWithLocations.add(
                OrganizationWithLocations(
                    organization = organization,
                    locations = locations
                )
            )
        }
        return organizationsWithLocations
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getOrganizationLocations(organizationId: UUID): Collection<LocationResponseDto> {
        val organization = organizationCache.getAllOrganizations()
            .find { it.id == organizationId }
            ?: throw RtsGenericException("The organization with id $organizationId does not exist")
        SessionContextProvider.initOrganization(organization)
        return locationCache.getAllLocations()
            .map { locationMapper.toResponseDto(it) }
    }
}
