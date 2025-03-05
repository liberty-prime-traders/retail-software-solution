package me.ezra_home.retail_software_solution.business.unitvalue

import com.google.common.base.Strings
import jakarta.transaction.Transactional
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueInsertDto
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueResponseDto
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.business.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Service
class UnitValueService(private val unitValueCache: UnitValueCache, private val unitValueMapper: UnitValueMapper) {

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
        val name = Optional.ofNullable(unitValueInsertDto.name)
        if (name.isEmpty || Strings.isNullOrEmpty(name.get())) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }
        val unitGroupId = unitValueInsertDto.unitGroupId
            ?: throw RtsGenericException(MISSING_UNITGROUP)
        val siblingUnitValues = unitValueCache.getByUnitGroupId(unitGroupId)
        val unitValueWithMatchingName = siblingUnitValues.find { it.name.equals(unitValueInsertDto.name, ignoreCase = true) }
        if (unitValueWithMatchingName != null) {
            throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name.get()))
        }
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
        if (name == null || name.isEmpty || Strings.isNullOrEmpty(name.get())) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }
        val unitGroupId = unitValueUpdateDto.unitGroupId
        if (unitGroupId == null || unitGroupId.isEmpty) {
            throw RtsGenericException(MISSING_UNITGROUP)
        }
        val siblingUnitValues = unitValueCache.getByUnitGroupId(unitGroupId.get())
        val unitValueWithMatchingName = siblingUnitValues.find {
            it.name.equals(name.get(), ignoreCase=true) && !Objects.equals(it.id, unitValueUpdateDto.id)
        }
        if (unitValueWithMatchingName != null) {
            throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name.get()))
        }
    }

    @Transactional
    fun deleteUnitValue(id: UUID?) {
        unitValueCache.deleteUnitValue(id)
    }

    companion object {
        const val NAME_IS_REQUIRED = "A unit value must have a name"
        const val MISSING_UNITGROUP = "A unitv alue cannot be saved without an unit group Id"
        const val NAME_ALREADY_EXISTS = "A unit value with the name %s is already assigned to the given unit group"
    }
}
