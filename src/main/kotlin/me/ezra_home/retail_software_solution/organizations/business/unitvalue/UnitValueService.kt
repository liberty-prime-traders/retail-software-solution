package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueUpdateDto
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

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getUnitValuesForUnitGroup(unitGroupId: UUID): Collection<UnitValueResponseDto> {
        return unitValueCache.getByUnitGroupId(unitGroupId).map {
            unitValueMapper.toResponseDto(it)
        }
    }

    fun createUnitValue(unitValueInsertDto: UnitValueInsertDto): UnitValueResponseDto {
        unitValueValidator.validateUnitValueInsert(unitValueInsertDto)
        val unitValueEntity = unitValueMapper.toEntity(unitValueInsertDto)
        unitValueCache.upsertUnitValue(unitValueEntity)
        return unitValueMapper.toResponseDto(unitValueEntity)
    }

    fun updateUnitValue(unitValueUpdateDto: UnitValueUpdateDto): UnitValueResponseDto {
        unitValueValidator.validateUnitValueUpdate(unitValueUpdateDto)
        val unitValueFromDb = unitValueCache.getAllUnitValues().find { Objects.equals(it.id, unitValueUpdateDto.id) }
        if (unitValueFromDb == null) {
            throw UpdatingNonExistingRecordException()
        }
        unitValueMapper.partialUpdate(unitValueUpdateDto, unitValueFromDb)
        unitValueCache.upsertUnitValue(unitValueFromDb)
        return unitValueMapper.toResponseDto(unitValueFromDb)
    }

    fun deleteUnitValue(id: UUID?) {
        unitValueCache.getAllUnitValues()
            .find { it.id == id }
            ?.apply { unitValueCache.deleteUnitValue(id) }
    }
}
