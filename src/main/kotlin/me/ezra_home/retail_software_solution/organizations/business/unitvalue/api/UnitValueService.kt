package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitName
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueCache
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueMapper
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueValidator
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class UnitValueService(
    private val unitValueCache: UnitValueCache,
    private val unitValueMapper: UnitValueMapper,
    private val unitValueValidator: UnitValueValidator
) {

    @UnitName
    fun getUnitName(unitValueId: UUID?): String? = unitValueId?.let { getUnitNamesById()[it] }

    fun getUnitNamesById(): Map<UUID, String> = unitValueCache.getUnitNamesById()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getUnitValuesForUnitGroup(unitGroupId: UUID): Collection<UnitValueResponseDto> {
        val unitNamesById = unitValueCache.getUnitNamesById()
        return unitValueCache.getByUnitGroupId(unitGroupId).map {
            unitValueMapper.toResponseDto(it, unitNamesById[it.baseUnit])
        }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllUnitValues(): Collection<UnitValueResponseDto> {
        val unitNamesById = unitValueCache.getUnitNamesById()
        return unitValueCache.getAllUnitValues().map {
            unitValueMapper.toResponseDto(it, unitNamesById[it.baseUnit])
        }
    }

    fun createUnitValue(unitValueInsertDto: UnitValueInsertDto): UnitValueResponseDto {
        unitValueValidator.validateUnitValueInsert(unitValueInsertDto)
        val dto = unitValueCache.create(unitValueInsertDto)
        return unitValueMapper.toResponseDto(dto, unitValueCache.getUnitNamesById()[dto.baseUnit])
    }

    fun updateUnitValue(unitValueUpdateDto: UnitValueUpdateDto): UnitValueResponseDto {
        unitValueValidator.validateUnitValueUpdate(unitValueUpdateDto)
        val existing = unitValueCache.getAllUnitValues().find { Objects.equals(it.id, unitValueUpdateDto.id) }
            ?: throw UpdatingNonExistingRecordException()
        val updated = unitValueUpdateDto.applyTo(existing)
        val saved = unitValueCache.save(updated)
        return unitValueMapper.toResponseDto(saved, unitValueCache.getUnitNamesById()[saved.baseUnit])
    }

    fun deleteUnitValue(id: UUID?) {
        unitValueCache.getAllUnitValues()
            .find { it.id == id }
            ?.apply { unitValueCache.deleteUnitValue(id) }
    }
}
