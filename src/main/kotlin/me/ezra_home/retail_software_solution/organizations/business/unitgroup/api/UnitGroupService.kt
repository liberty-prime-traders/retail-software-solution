package me.ezra_home.retail_software_solution.organizations.business.unitgroup.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupCache
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupMapper
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllUnitGroups(): Collection<UnitGroupResponseDto> {
        return unitGroupCache.getAllUnitGroups().map { unitGroupMapper.toResponseDto(it) }
    }

    fun createUnitGroup(unitGroupInsertDto: UnitGroupInsertDto): UnitGroupResponseDto {
        validateNameOnSave(Optional.ofNullable(unitGroupInsertDto.name))
        val dto = unitGroupMapper.toDomainDto(unitGroupInsertDto)
        unitGroupCache.upsertUnitGroup(dto)
        return unitGroupMapper.toResponseDto(dto)
    }

    private fun validateNameOnSave(optionalName: Optional<String>?, id: UUID? = null) {
        val name = StringUtils.getValueOrException(optionalName, "An UnitGroup must have a name")
        unitGroupCache.getAllUnitGroups()
            .find { StringUtils.isEquivalent(it.name, name) && !Objects.equals(it.id, id) }
            ?.let { throw RtsGenericException("An UnitGroup using the name '$name' already exists") }
    }

    fun updateUnitGroup(unitGroupUpdateDto: UnitGroupUpdateDto): UnitGroupResponseDto {
        val id = unitGroupUpdateDto.id ?: throw QueriedByEmptyIdException()
        val dto = unitGroupCache.getAllUnitGroups().find { it.id == id } ?: throw NotFoundException()
        validateNameOnSave(unitGroupUpdateDto.name, unitGroupUpdateDto.id)
        unitGroupMapper.partialUpdate(unitGroupUpdateDto, dto)
        unitGroupCache.upsertUnitGroup(dto)
        return unitGroupMapper.toResponseDto(dto)
    }

    fun deleteUnitGroup(id: UUID) {
        unitGroupCache.getAllUnitGroups()
            .find { it.id == id }
            ?.apply { unitGroupCache.deleteUnitGroup(id) }
    }
}
