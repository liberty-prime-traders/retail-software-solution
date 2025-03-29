package me.ezra_home.retail_software_solution.business.unitvalue

import com.google.common.base.Strings
import me.ezra_home.retail_software_solution.business.unitgroup.UnitGroupCache
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueInsertDto
import me.ezra_home.retail_software_solution.business.unitvalue.dto.UnitValueUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.Objects

@Component
class UnitValueValidator(
    private val unitValueCache: UnitValueCache,
    private val unitGroupCache: UnitGroupCache
) {

    fun validateUnitValueInsert(unitValueInsertDto: UnitValueInsertDto) {
        if (Strings.isNullOrEmpty(unitValueInsertDto.name)) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }
        if (Strings.isNullOrEmpty(unitValueInsertDto.code)) {
            throw RtsGenericException(CODE_IS_REQUIRED)
        }
        if (unitValueInsertDto.unitGroupId == null) {
            throw RtsGenericException(UNIT_GROUP_ID_IS_REQUIRED)
        }
        if (unitGroupCache.getAllUnitGroups().find { it.id == unitValueInsertDto.unitGroupId } == null) {
            throw RtsGenericException(PROVIDED_MISSING_UNIT_GROUP)
        }

        unitValueCache.getAllUnitValues().find { it.name.equals(unitValueInsertDto.name, ignoreCase = true) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, unitValueInsertDto.name)) }

        if(unitValueInsertDto.baseUnit != null && unitValueInsertDto.conversionFactor == null){
            throw RtsGenericException(CONVERSION_FACTOR_IS_REQUIRED)
        }

        val baseUnitExists = unitValueCache.getByUnitGroupId(unitValueInsertDto.unitGroupId)
            .any { it.id == unitValueInsertDto.baseUnit }

        if (unitValueInsertDto.baseUnit != null && !baseUnitExists) {
            throw RtsGenericException(BASE_UNIT_MUST_BE_IN_GROUP)
        }

    }

    fun validateUnitValueUpdate(unitValueUpdateDto: UnitValueUpdateDto) {
        val name = unitValueUpdateDto.name?.get()
        if (Strings.isNullOrEmpty(name)) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }
        if (Strings.isNullOrEmpty(unitValueUpdateDto.code?.get())) {
            throw RtsGenericException(CODE_IS_REQUIRED)
        }

        if(unitGroupCache.getAllUnitGroups().find { it.id == unitValueUpdateDto.unitGroupId?.get()} == null){
            throw RtsGenericException(PROVIDED_MISSING_UNIT_GROUP)
        }

        unitValueCache.getAllUnitValues().find { it.name.equals(name, ignoreCase = true) && !Objects.equals(it.id, unitValueUpdateDto.id) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }

        if(unitValueUpdateDto.baseUnit != null && unitValueUpdateDto.conversionFactor == null) {
            throw RtsGenericException(CONVERSION_FACTOR_IS_REQUIRED)
        }

        if(unitValueCache.getByUnitGroupId(unitValueUpdateDto.unitGroupId?.get()).find { it.baseUnit == unitValueUpdateDto.baseUnit?.get() } == null){
            throw RtsGenericException(BASE_UNIT_MUST_BE_IN_GROUP)
        }
    }

    companion object {
        const val NAME_IS_REQUIRED = "A unit value must have a name"
        const val CODE_IS_REQUIRED = "A unit code must have a name"
        const val UNIT_GROUP_ID_IS_REQUIRED = "A unit value must have a unit group id"
        const val CONVERSION_FACTOR_IS_REQUIRED = "A unit value with a base unit must have a conversion factor"
        const val PROVIDED_MISSING_UNIT_GROUP = "UnitGroup with the provided id does not exist"
        const val NAME_ALREADY_EXISTS = "A unit value with the name %s already exists"
        const val BASE_UNIT_MUST_BE_IN_GROUP = "The base unit must be selected from the assigned group"
    }
}
