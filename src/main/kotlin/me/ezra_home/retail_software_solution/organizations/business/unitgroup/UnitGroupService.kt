package me.ezra_home.retail_software_solution.organizations.business.unitgroup


import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupUpdateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.service.OrganizationReferenceNumberGeneratorService
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class UnitGroupService(
    private val unitGroupMapper: UnitGroupMapper,
    private val unitGroupCache: UnitGroupCache,
    private val referenceNumberGeneratorService: OrganizationReferenceNumberGeneratorService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllUnitGroups(): Collection<UnitGroupResponseDto> {
        return unitGroupCache.getAllUnitGroups().map { unitGroupMapper.toResponseDto(it) }
    }

    fun createUnitGroup(unitGroupInsertDto: UnitGroupInsertDto): UnitGroupResponseDto {
        validateNameOnSave(Optional.ofNullable(unitGroupInsertDto.name))
        val entity = unitGroupMapper.toEntity(unitGroupInsertDto)
        entity.referenceNumber = referenceNumberGeneratorService.generateReferenceNumber(TableNames.UNIT_GROUP)
        unitGroupCache.upsertUnitGroup(entity)
        return unitGroupMapper.toResponseDto(entity)
    }

    private fun validateNameOnSave(optionalName: Optional<String>?, id: UUID? = null) {
        val name = StringUtils.getValueOrException(optionalName, "An UnitGroup must have a name")
        unitGroupCache.getAllUnitGroups()
            .find { StringUtils.isEquivalent(it.name, name) && !Objects.equals(it.id, id) }
            ?.let { throw RtsGenericException("An UnitGroup using the name '$name' already exists") }
    }

    fun updateUnitGroup(unitGroupUpdateDto: UnitGroupUpdateDto): UnitGroupResponseDto {
        val id = unitGroupUpdateDto.id ?: throw QueriedByEmptyIdException()
        val entityFromDatabase = unitGroupCache.getAllUnitGroups().find { it.id == id } ?: throw NotFoundException()
        validateNameOnSave(unitGroupUpdateDto.name, unitGroupUpdateDto.id)
        unitGroupMapper.partialUpdate(unitGroupUpdateDto, entityFromDatabase)
        unitGroupCache.upsertUnitGroup(entityFromDatabase)
        return unitGroupMapper.toResponseDto(entityFromDatabase)
    }

    fun deleteUnitGroup(id: UUID?) {
        id?.let {
            unitGroupCache.getAllUnitGroups().find { it.id == id }?.let { entity ->
                val usageCount = entity.usageCount
                if (usageCount > 0L) {
                    throw RtsGenericException("UnitGroup ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }
                unitGroupCache.deleteUnitGroup(id)
            }
        }
    }
}
