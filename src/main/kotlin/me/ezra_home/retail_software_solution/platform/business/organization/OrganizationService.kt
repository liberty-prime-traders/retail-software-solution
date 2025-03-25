package me.ezra_home.retail_software_solution.platform.business.organization

import jakarta.transaction.Transactional
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationUpdateDto
import me.ezra_home.retail_software_solution.platform.business.subdomain.SubdomainService
import me.ezra_home.retail_software_solution.util.enums.Status
import me.ezra_home.retail_software_solution.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Service
class OrganizationService(
    private val organizationMapper: OrganizationMapper,
    private val organizationCache: OrganizationCache,
    private val subdomainService: SubdomainService
) {

    @Transactional
    fun getAllOrganizations(): Collection<OrganizationResponseDto> {
        return organizationCache.getAllOrganizations().map { organizationMapper.toResponseDto(it) }
    }

    @Transactional
    fun createOrganization(organizationInsertDto: OrganizationInsertDto): OrganizationResponseDto {
        validateNameOnSave(Optional.ofNullable(organizationInsertDto.name))
        markSubdomainAsUsed(organizationInsertDto)
        val entity = organizationMapper.toEntity(organizationInsertDto)
        organizationCache.upsertOrganization(entity)
        return organizationMapper.toResponseDto(entity)
    }

    private fun validateNameOnSave(name: Optional<String>?, id: UUID? = null) {
        if (name == null || name.isEmpty || name.get().isBlank()) {
            throw RtsGenericException("An Organization must have a name")
        }
        val organizationWithMatchingName = organizationCache.getAllOrganizations().find {
            it.name.equals(name.get(), ignoreCase = true) && !Objects.equals(it.id, id)
        }
        if (organizationWithMatchingName != null) {
            throw RtsGenericException("An organization using the name '${name.get()}' already exists")
        }
    }

    private fun markSubdomainAsUsed(organizationInsertDto: OrganizationInsertDto) {
        val intendedSubdomain = organizationInsertDto.subdomain
        if (intendedSubdomain.isNullOrBlank()) {
            throw RtsGenericException("An Organization must have a subdomain")
        }
        val reservedSubdomain = subdomainService.getReservedSubdomains()
            .find { it.subdomain == intendedSubdomain && it.status == Status.UNUSED }
        if (reservedSubdomain == null) {
            throw RtsGenericException("Subdomain '$intendedSubdomain' was not reserved")
        }
        subdomainService.markSubdomainAsUsed(reservedSubdomain.id)
    }

    @Transactional
    fun updateOrganization(organizationUpdateDto: OrganizationUpdateDto): OrganizationResponseDto {
        val id = organizationUpdateDto.id ?: throw QueriedByEmptyIdException()
        val entityFromDatabase = organizationCache.getAllOrganizations().find { it.id == id } ?: throw NotFoundException()
        validateNameOnSave(organizationUpdateDto.name, organizationUpdateDto.id)
        organizationMapper.partialUpdate(organizationUpdateDto, entityFromDatabase)
        organizationCache.upsertOrganization(entityFromDatabase)
        return organizationMapper.toResponseDto(entityFromDatabase)
    }

    @Transactional
    fun deleteOrganization(id: UUID?) {
        id?.let {
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
