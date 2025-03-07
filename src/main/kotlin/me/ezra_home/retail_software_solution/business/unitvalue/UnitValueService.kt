package me.ezra_home.retail_software_solution.business.unitvalue

import com.google.common.base.Strings
import jakarta.transaction.Transactional
import me.ezra_home.retail_software_solution.business.unitgroup.UnitGroupCache
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueInsertDto
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueResponseDto
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.business.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.UUID

@Service
class UnitValueService(
    private val unitValueCache: UnitValueCache,
    private val unitValueMapper: UnitValueMapper,
    private val unitGroupCache: UnitGroupCache
) {

    @Transactional
    fun getUnitValuesForUnitGroup(unitGroupId: UUID): Collection<UnitValueResponseDto> {
        return unitValueCache.getByUnitGroupId(unitGroupId).map {
            unitValueMapper.toResponseDto(it)
        }
    }

    @Transactional
    fun createUnitValue(unitValueInsertDto: UnitValueInsertDto): UnitValueResponseDto {
        validateUnitValueInsert(unitValueInsertDto)
        val unitValueEntity = unitValueMapper.toEntity(unitValueInsertDto)
        unitValueCache.upsertUnitValue(unitValueEntity)
        return unitValueMapper.toResponseDto(unitValueEntity)
    }

    private fun validateUnitValueInsert(unitValueInsertDto: UnitValueInsertDto) {
        if (Strings.isNullOrEmpty(unitValueInsertDto.name)) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }
        if (Strings.isNullOrEmpty(unitValueInsertDto.code)) {
            throw RtsGenericException(CODE_IS_REQUIRED)
        }
        val unitGroupId = unitValueInsertDto.unitGroupId
        val siblingUnitValues = unitValueCache.getByUnitGroupId(unitGroupId)
        val unitValueWithMatchingName = siblingUnitValues.find { it.name.equals(unitValueInsertDto.name, ignoreCase = true) }
        if (unitValueWithMatchingName != null) {
            throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, unitValueInsertDto.name))
        }

        if(unitValueInsertDto.baseUnit != null && unitValueInsertDto.conversionFactor == null){
            throw RtsGenericException("Conversion factor required for the base unit.")
        }

        if(unitValueCache.getAllUnitValues().find { it.baseUnit == unitValueInsertDto.baseUnit } == null){
            throw RtsGenericException("The base unit does not exist.")
        }

        val unitGroup = unitGroupCache.getAllUnitGroups().find { it.id == unitValueInsertDto.unitGroupId }
        if(unitGroup == null){
            throw RtsGenericException("UnitGroup with the provided id does not exist")
        }
        unitGroup.usageCount += 1L
    }

    @Transactional
    fun updateUnitValue(unitValueUpdateDto: UnitValueUpdateDto): UnitValueResponseDto {
        validateUnitValueUpdate(unitValueUpdateDto)
        val unitValueEntity = unitValueCache.getByUnitGroupId(unitValueUpdateDto.unitGroupId?.get())
            .find { Objects.equals(it.id, unitValueUpdateDto.id) }
        if (unitValueEntity == null) {
            throw UpdatingNonExistingRecordException()
        }
        unitValueMapper.partialUpdate(unitValueUpdateDto, unitValueEntity)
        return unitValueMapper.toResponseDto(unitValueEntity)
    }

    private fun validateUnitValueUpdate(unitValueUpdateDto: UnitValueUpdateDto) {
        val name = unitValueUpdateDto.name
        if (Strings.isNullOrEmpty(name.toString())) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }

        if (Strings.isNullOrEmpty(unitValueUpdateDto.code.toString())) {
            throw RtsGenericException(CODE_IS_REQUIRED)
        }

        val unitGroupId = unitValueUpdateDto.unitGroupId

        val siblingUnitValues = unitValueCache.getByUnitGroupId(unitGroupId?.get())
        val unitValueWithMatchingName = siblingUnitValues.find {
            it.name.equals(name.toString(),ignoreCase = true) && !Objects.equals(it.id, unitValueUpdateDto.id)
        }
        if (unitValueWithMatchingName != null) {
            throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name.toString()))
        }

        if(unitValueUpdateDto.baseUnit != null && unitValueUpdateDto.conversionFactor == null){
            throw RtsGenericException("Conversion factor required for the base unit.")
        }

        if(unitGroupId?.get() != unitValueUpdateDto.unitGroupId?.get()){
            val oldUnitGroup = unitGroupCache.getAllUnitGroups().find { it.id == unitGroupId?.get() }
            val newUnitGroup = unitGroupCache.getAllUnitGroups().find { it.id == unitValueUpdateDto.unitGroupId?.get()}
            if(newUnitGroup == null){
                throw RtsGenericException("UnitGroup with the provided id does not exist")
            }
            oldUnitGroup?.usageCount?.minus(1L)
            newUnitGroup.usageCount += 1L
        }
    }

    @Transactional
    fun deleteUnitValue(id: UUID?) {
        if (id != null) {
            val entity = unitValueCache.getAllUnitValues().find { it.id == id }
            if (entity != null) {
                val usageCount = entity.usageCount
                if (usageCount > 0) {
                    throw RtsGenericException("UnitValue ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }

                val unitGroup = unitGroupCache.getAllUnitGroups().find { it.id == entity.unitGroupId }
                if(unitGroup != null){
                    unitGroup.usageCount -= 1L
                }

                unitValueCache.deleteUnitValue(id)

            }

        }
    }

    companion object {
        const val NAME_IS_REQUIRED = "A unit value must have a name"
        const val CODE_IS_REQUIRED = "A unit code must have a name"
        const val NAME_ALREADY_EXISTS = "A unit value with the name %s is already assigned to the given unit group"
    }
}
