package me.ezra_home.retail_software_solution.organizations.business.unitvalue.public

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
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

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getUnitValuesForUnitGroup(unitGroupId: UUID): Collection<UnitValueResponseDto> {
        return unitValueCache.getByUnitGroupId(unitGroupId).map {
            unitValueMapper.toResponseDto(it)
        }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllUnitValues(): Collection<UnitValueResponseDto> {
        return unitValueCache.getAllUnitValues().map {
            unitValueMapper.toResponseDto(it)
        }
    }

    fun createUnitValue(unitValueInsertDto: UnitValueInsertDto): UnitValueResponseDto {
        unitValueValidator.validateUnitValueInsert(unitValueInsertDto)
        val dto = unitValueMapper.toDomainDto(unitValueInsertDto)
        unitValueCache.upsertUnitValue(dto)
        return unitValueMapper.toResponseDto(dto)
    }

    fun updateUnitValue(unitValueUpdateDto: UnitValueUpdateDto): UnitValueResponseDto {
        unitValueValidator.validateUnitValueUpdate(unitValueUpdateDto)
        val dto = unitValueCache.getAllUnitValues().find { Objects.equals(it.id, unitValueUpdateDto.id) }
            ?: throw UpdatingNonExistingRecordException()
        unitValueMapper.partialUpdate(unitValueUpdateDto, dto)
        unitValueCache.upsertUnitValue(dto)
        return unitValueMapper.toResponseDto(dto)
    }

    fun deleteUnitValue(id: UUID?) {
        unitValueCache.getAllUnitValues()
            .find { it.id == id }
            ?.apply { unitValueCache.deleteUnitValue(id) }
    }
}
