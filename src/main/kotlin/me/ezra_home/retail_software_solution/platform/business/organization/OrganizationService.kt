package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.organizations.business.location.LocationMapper
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationResponseDto
import me.ezra_home.retail_software_solution.organizations.business.organization_user.OrganizationUserService
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.OrganizationAdminCache
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.OrganizationAdminService
import me.ezra_home.retail_software_solution.organizations.model.OrganizationAdminEntity
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationUpsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationWithLocations
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.OrganizationJoinRequestMapper
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.OrganizationJoinRequestService
import me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto.OrganizationLaunchResponseDto
import me.ezra_home.retail_software_solution.platform.business.reserved_subdomain.ReservedSubdomainService
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.prefix_configuration.OrganizationPrefixConfigurationService
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryService
import me.ezra_home.retail_software_solution.util.service.OrganizationReferenceNumberGeneratorService
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.enums.Status
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.model.TableNames
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
    private val locationMapper: LocationMapper,
    private val tableRegistryService: TableRegistryService,
    private val organizationPrefixConfigurationService: OrganizationPrefixConfigurationService,
    private val organizationReferenceNumberGeneratorService: OrganizationReferenceNumberGeneratorService
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllOrganizations(): Collection<OrganizationResponseDto> {
        return organizationCache.getAllOrganizations().map { organizationMapper.toResponseDto(it) }
    }

    fun createOrganization(organizationInsertDto: OrganizationUpsertDto): OrganizationResponseDto {
        organizationValidator.validateNameOnSave(organizationInsertDto.name)
        markSubdomainAsUsed(organizationInsertDto)
        val schemaName = createOrganizationSchema(organizationInsertDto.subdomain!!)
        try {
            SessionContextProvider.getSession().organizationSchemaName = schemaName
            val entity = organizationMapper.toEntity(organizationInsertDto).apply {
                this.schemaName = schemaName
                this.referenceNumber = organizationReferenceNumberGeneratorService.generateReferenceNumber(
                    TableNames.ORGANIZATION
                )
            }
            organizationCache.upsertOrganization(entity)
            val userId = SessionContextProvider.getUserId()
            val registryRecords = tableRegistryService.getAllForSchemaLevel(SchemaLevel.ORGANIZATION)
            organizationPrefixConfigurationService.getPrefixForTableName(TableNames.ORGANIZATION)
                ?: throw RtsGenericException("Table prefix not found for table: ${TableNames.ORGANIZATION}")
            registryRecords.forEach { registryRecord ->
                organizationPrefixConfigurationService.bulkCreateForRegistry(
                    registryRecord.id!!,
                    registryRecord.defaultPrefix!!,
                    userId
                )
            }
            organizationAdminCache.upsertOrganizationAdmin(OrganizationAdminEntity(entity.createdById))
            return organizationMapper.toResponseDto(entity)
        } catch (e: Exception) {
            organizationSchemaService.dropSchema(schemaName)
            throw e
        }

    }

    private fun createOrganizationSchema(subdomain: String): String {
        val schemaName = "org_${subdomain.lowercase().replace("-", "_")}"
        organizationSchemaService.createSchema(schemaName)
        return schemaName
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
        val entityFromDatabase = organizationCache.getAllOrganizations().find { it.id == organizationId }
            ?: throw NotFoundException()
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
