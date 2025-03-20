package me.ezra_home.retail_software_solution.business.unitvalue

import jakarta.transaction.Transactional
import me.ezra_home.retail_software_solution.business.unitgroup.UnitGroupCache
import me.ezra_home.retail_software_solution.business.unitgroup.UnitGroupUsageCountManager
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueInsertDto
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueResponseDto
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.business.util.exceptions.UpdatingNonExistingRecordException
import me.ezra_home.retail_software_solution.model.entity.UnitValueEntity
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
class UnitValueService(
    private val unitValueCache: UnitValueCache,
    private val unitValueMapper: UnitValueMapper,
    private val unitGroupCache: UnitGroupCache,
    private val unitValueValidator: UnitValueValidator,
    private val unitGroupUsageCountManager: UnitGroupUsageCountManager
) {

    @Transactional
    fun getUnitValuesForUnitGroup(unitGroupId: UUID): Collection<UnitValueResponseDto> {
        return unitValueCache.getByUnitGroupId(unitGroupId).map {
            unitValueMapper.toResponseDto(it)
        }
    }

    @Transactional
    fun createUnitValue(unitValueInsertDto: UnitValueInsertDto): UnitValueResponseDto {
        unitValueValidator.validateUnitValueInsert(unitValueInsertDto)
        val unitValueEntity = unitValueMapper.toEntity(unitValueInsertDto)
        unitGroupUsageCountManager.incrementUsageCount(unitValueInsertDto.unitGroupId)
        unitValueCache.upsertUnitValue(unitValueEntity)
        return unitValueMapper.toResponseDto(unitValueEntity)
    }

    @Transactional
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
                unitGroupUsageCountManager.decrementUsageCount(it)
            }
            unitGroupCache.getAllUnitGroups().find { it.id == unitValueUpdateDto.unitGroupId?.get() }?.let {
                unitGroupUsageCountManager.incrementUsageCount(it)
            }
        }
    }

    private fun unitGroupIsChanging(unitValueUpdateDto: UnitValueUpdateDto, unitValueFromDb: UnitValueEntity): Boolean {
        return unitValueUpdateDto.unitGroupId!!.get() != unitValueFromDb.unitGroupId
    }

    @Transactional
    fun deleteUnitValue(id: UUID?) {
        id?.let {
            unitValueCache.getAllUnitValues().find { it.id == id }?.let {entity ->
                val usageCount = entity.usageCount
                if (usageCount > 0) {
                    throw RtsGenericException("UnitValue ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }
                unitGroupCache.getAllUnitGroups().find { it.id == entity.unitGroupId }?.let {
                    unitGroupUsageCountManager.decrementUsageCount(it)
                }
                unitValueCache.deleteUnitValue(id)
            }
        }
    }
}
