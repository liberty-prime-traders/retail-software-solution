package me.ezra_home.retail_software_solution.business.organization

import com.google.common.base.Strings
import jakarta.transaction.Transactional
import me.ezra_home.retail_software_solution.business.organization.dto.OrganizationInsertDto
import me.ezra_home.retail_software_solution.business.organization.dto.OrganizationResponseDto
import me.ezra_home.retail_software_solution.business.organization.dto.OrganizationUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Service
class OrganizationService(
    private val organizationMapper: OrganizationMapper,
    private val organizationCache: OrganizationCache
) {

    @Transactional
    fun getAllOrganizations(): Collection<OrganizationResponseDto> {
        return organizationCache.getAllOrganizations().map { organizationMapper.toResponseDto(it) }
    }

    @Transactional
    fun createOrganization(organizationInsertDto: OrganizationInsertDto): OrganizationResponseDto {
        validateNameOnSave(Optional.ofNullable(organizationInsertDto.name))
        val entity = organizationMapper.toEntity(organizationInsertDto)
        organizationCache.upsertOrganization(entity)
        return organizationMapper.toResponseDto(entity)
    }

    private fun validateNameOnSave(name: Optional<String>?, id: UUID? = null) {
        if (name == null || name.isEmpty || Strings.isNullOrEmpty(name.get())) {
            throw RtsGenericException("An Organization must have a name")
        }
        val organizationWithMatchingName = organizationCache.getAllOrganizations().find {
            it.name == name.get() && !Objects.equals(it.id, id)
        }
        if (organizationWithMatchingName != null) {
            throw RtsGenericException("An organization using the name '${name.get()}' already exists")
        }
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
        if (id != null) {
            val entity = organizationCache.getAllOrganizations().find { it.id == id }
            if (entity != null) {
                val usageCount = entity.usageCount
                if (usageCount != null && usageCount > 0L) {
                    throw RtsGenericException("Organization ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }
                organizationCache.deleteOrganization(id)
            }

        }
    }
}
