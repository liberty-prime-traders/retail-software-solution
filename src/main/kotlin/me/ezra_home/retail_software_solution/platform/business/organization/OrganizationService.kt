package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationUpdateDto
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.OrganizationAdminService
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.OrganizationAdminValidator
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.dto.OrganizationAdminInsertDto
import me.ezra_home.retail_software_solution.platform.business.subdomain.ReservedSubdomainService
import me.ezra_home.retail_software_solution.util.enums.Status
import me.ezra_home.retail_software_solution.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class OrganizationService(
    private val organizationMapper: OrganizationMapper,
    private val organizationCache: OrganizationCache,
    private val reservedSubdomainService: ReservedSubdomainService,
    private val organizationAdminService: OrganizationAdminService,
    private val organizationValidator: OrganizationValidator,
    private val organizationAdminValidator: OrganizationAdminValidator
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllOrganizations(): Collection<OrganizationResponseDto> {
        return organizationCache.getAllOrganizations().map { organizationMapper.toResponseDto(it) }
    }

    fun createOrganization(organizationInsertDto: OrganizationInsertDto): OrganizationResponseDto {
        organizationValidator.validateNameOnSave(Optional.ofNullable(organizationInsertDto.name))
        markSubdomainAsUsed(organizationInsertDto)
        val entity = organizationMapper.toEntity(organizationInsertDto)
        organizationCache.upsertOrganization(entity)
        organizationAdminService.createOrganizationAdmin(OrganizationAdminInsertDto(entity.id, entity.createdById))
        return organizationMapper.toResponseDto(entity)
    }

    private fun markSubdomainAsUsed(organizationInsertDto: OrganizationInsertDto) {
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

    fun updateOrganization(organizationUpdateDto: OrganizationUpdateDto): OrganizationResponseDto {
        val id = organizationUpdateDto.id ?: throw QueriedByEmptyIdException()
        organizationAdminValidator.canCurrentUserOperateOnOrganization(id)
        organizationValidator.validateNameOnSave(organizationUpdateDto.name, organizationUpdateDto.id)
        val entityFromDatabase = organizationCache.getAllOrganizations().find { it.id == id } ?: throw NotFoundException()
        organizationMapper.partialUpdate(organizationUpdateDto, entityFromDatabase)
        organizationCache.upsertOrganization(entityFromDatabase)
        return organizationMapper.toResponseDto(entityFromDatabase)
    }

    fun deleteOrganization(id: UUID?) {
        id?.let {
            organizationAdminValidator.canCurrentUserOperateOnOrganization(id)
            organizationCache.getAllOrganizations().find { it.id == id }?.let {entity ->
                val usageCount = entity.usageCount
                if (usageCount > 0L) {
                    throw RtsGenericException("Organization ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }
                organizationCache.deleteOrganization(id)
            }
        }
    }
}
