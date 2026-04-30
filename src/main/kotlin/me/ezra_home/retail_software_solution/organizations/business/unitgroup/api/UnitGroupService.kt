package me.ezra_home.retail_software_solution.organizations.business.unitgroup.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupCache
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupMapper
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
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
    fun getAllUnitGroupDtos(): Collection<UnitGroupDto> = unitGroupCache.getAllUnitGroups()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllUnitGroups(): Collection<UnitGroupResponseDto> {
        return unitGroupCache.getAllUnitGroups().map { unitGroupMapper.toResponseDto(it) }
    }

    fun createUnitGroup(unitGroupInsertDto: UnitGroupInsertDto): UnitGroupResponseDto {
        validateNameOnSave(Optional.ofNullable(unitGroupInsertDto.name))
        val dto = unitGroupCache.create(unitGroupInsertDto)
        return unitGroupMapper.toResponseDto(dto)
    }

    private fun validateNameOnSave(optionalName: Optional<String>?, id: UUID? = null) {
        val name = StringUtils.getValueOrException(optionalName, "An UnitGroup must have a name")
        unitGroupCache.getAllUnitGroups()
            .find { StringUtils.isEquivalent(it.name, name) && !Objects.equals(it.id, id) }
            ?.let { throw RtsGenericException("An UnitGroup using the name '$name' already exists") }
    }

    fun updateUnitGroup(unitGroupUpdateDto: UnitGroupUpdateDto): UnitGroupResponseDto {
        val existing = unitGroupCache.getAllUnitGroups().find { it.id == unitGroupUpdateDto.id }
            ?: throw UpdatingNonExistingRecordException()
        if (existing.systemDefined) throw RtsGenericException("System-defined unit groups cannot be modified")
        validateNameOnSave(unitGroupUpdateDto.name, unitGroupUpdateDto.id)
        val updated = unitGroupUpdateDto.applyTo(existing)
        val saved = unitGroupCache.save(updated)
        return unitGroupMapper.toResponseDto(saved)
    }

    fun deleteUnitGroup(id: UUID) {
        unitGroupCache.getAllUnitGroups().find { it.id == id }
            ?.apply {
                if (this.systemDefined) throw RtsGenericException("System-defined unit groups cannot be deleted")
                unitGroupCache.deleteUnitGroup(id)
            }
    }
}
