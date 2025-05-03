package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupCache
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupUsageCounter
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.UnitValueEntity
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class UnitValueService(
    private val unitValueCache: UnitValueCache,
    private val unitValueMapper: UnitValueMapper,
    private val unitGroupCache: UnitGroupCache,
    private val unitValueValidator: UnitValueValidator,
    private val unitGroupUsageCounter: UnitGroupUsageCounter
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
        unitGroupUsageCounter.incrementUsageCount(unitValueInsertDto.unitGroupId)
        unitValueCache.upsertUnitValue(unitValueEntity)
        return unitValueMapper.toResponseDto(unitValueEntity)
    }

    fun updateUnitValue(unitValueUpdateDto: UnitValueUpdateDto): UnitValueResponseDto {
        unitValueValidator.validateUnitValueUpdate(unitValueUpdateDto)
        val unitValueFromDb = unitValueCache.getAllUnitValues().find { Objects.equals(it.id, unitValueUpdateDto.id) }
        if (unitValueFromDb == null) {
            throw UpdatingNonExistingRecordException()
        }
        updateUnitGroupUsageCount(unitValueFromDb, unitValueUpdateDto)
        unitValueMapper.partialUpdate(unitValueUpdateDto, unitValueFromDb)
        unitValueCache.upsertUnitValue(unitValueFromDb)
        return unitValueMapper.toResponseDto(unitValueFromDb)
    }

    private fun updateUnitGroupUsageCount(unitValueFromDb: UnitValueEntity, unitValueUpdateDto: UnitValueUpdateDto) {
        if (unitGroupIsChanging(unitValueUpdateDto, unitValueFromDb)) {
            unitGroupCache.getAllUnitGroups().find { it.id == unitValueFromDb.unitGroupId }?.let {
                unitGroupUsageCounter.decrementUsageCount(it)
            }
            unitGroupCache.getAllUnitGroups().find { it.id == unitValueUpdateDto.unitGroupId?.get() }?.let {
                unitGroupUsageCounter.incrementUsageCount(it)
            }
        }
    }

    private fun unitGroupIsChanging(unitValueUpdateDto: UnitValueUpdateDto, unitValueFromDb: UnitValueEntity): Boolean {
        return unitValueUpdateDto.unitGroupId!!.get() != unitValueFromDb.unitGroupId
    }

    fun deleteUnitValue(id: UUID?) {
        id?.let {
            unitValueCache.getAllUnitValues().find { it.id == id }?.let {entity ->
                val usageCount = entity.usageCount
                if (usageCount > 0) {
                    throw RtsGenericException("UnitValue ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }
                unitGroupCache.getAllUnitGroups().find { it.id == entity.unitGroupId }?.let {
                    unitGroupUsageCounter.decrementUsageCount(it)
                }
                unitValueCache.deleteUnitValue(id)
            }
        }
    }
}
