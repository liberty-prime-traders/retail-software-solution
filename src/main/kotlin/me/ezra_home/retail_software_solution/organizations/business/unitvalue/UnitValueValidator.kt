package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueUpdateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class UnitValueValidator(
    private val unitValueCache: UnitValueCache,
    private val unitGroupDataFetcher: UnitGroupDataFetcher
) {

    fun validateUnitValueInsert(unitValueInsertDto: UnitValueInsertDto) {
        if (unitValueInsertDto.name.isNullOrBlank()) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }
        if (unitValueInsertDto.code.isNullOrBlank()) {
            throw RtsGenericException(CODE_IS_REQUIRED)
        }
        if (unitValueInsertDto.unitGroupId == null) {
            throw RtsGenericException(UNIT_GROUP_ID_IS_REQUIRED)
        }
        if (!unitGroupDataFetcher.exists(unitValueInsertDto.unitGroupId)) {
            throw RtsGenericException(PROVIDED_MISSING_UNIT_GROUP)
        }
        unitValueCache.getByUnitGroupId(unitValueInsertDto.unitGroupId)
            .find { StringUtils.isEquivalent(it.name, unitValueInsertDto.name) }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, unitValueInsertDto.name)) }

        unitValueCache.getAllUnitValues().find { it.code == unitValueInsertDto.code }
            ?.let { throw RtsGenericException(String.format(CODE_ALREADY_EXISTS, unitValueInsertDto.code)) }

        if (unitValueInsertDto.baseUnit != null && unitValueInsertDto.conversionFactor == null){
            throw RtsGenericException(CONVERSION_FACTOR_IS_REQUIRED)
        }

        if (unitValueInsertDto.conversionFactor != null && unitValueInsertDto.baseUnit == null) {
            throw RtsGenericException(BASE_UNIT_IS_REQUIRED)
        }

        val baseUnitExistsInGroup = unitValueCache.getByUnitGroupId(unitValueInsertDto.unitGroupId)
            .any { it.id == unitValueInsertDto.baseUnit }

        if (unitValueInsertDto.baseUnit != null && !baseUnitExistsInGroup) {
            throw RtsGenericException(BASE_UNIT_MUST_BE_IN_GROUP)
        }
    }

    fun validateUnitValueUpdate(unitValueUpdateDto: UnitValueUpdateDto) {
        val name = unitValueUpdateDto.name?.get()
        if (name.isNullOrBlank()) {
            throw RtsGenericException(NAME_IS_REQUIRED)
        }
        val code = unitValueUpdateDto.code?.get()
        if (code.isNullOrBlank()) {
            throw RtsGenericException(CODE_IS_REQUIRED)
        }
        val allUnitValues = unitValueCache.getAllUnitValues()
        val existing = allUnitValues.find { it.id == unitValueUpdateDto.id }

        allUnitValues.find {
            it.unitGroupId == existing?.unitGroupId && StringUtils.isEquivalent(it.name, name) && it.id != unitValueUpdateDto.id
        }?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }

        allUnitValues.find { it.code == code && it.id != unitValueUpdateDto.id }
            ?.let { throw RtsGenericException(String.format(CODE_ALREADY_EXISTS, code)) }

        val baseUnitIsProvided = unitValueUpdateDto.baseUnit?.isPresent == true
        val conversionFactorIsProvided = unitValueUpdateDto.conversionFactor?.isPresent == true

        if (baseUnitIsProvided && !conversionFactorIsProvided) {
            throw RtsGenericException(CONVERSION_FACTOR_IS_REQUIRED)
        }

        if (conversionFactorIsProvided && !baseUnitIsProvided) {
            throw RtsGenericException(BASE_UNIT_IS_REQUIRED)
        }

        if (baseUnitIsProvided) {
            val baseUnitExistsInGroup = unitValueCache.getByUnitGroupId(existing?.unitGroupId)
                .any { it.id == unitValueUpdateDto.baseUnit?.get() }
            if (!baseUnitExistsInGroup) throw RtsGenericException(BASE_UNIT_MUST_BE_IN_GROUP)
        }
    }

    companion object {
        const val NAME_IS_REQUIRED = "A unit value must have a name"
        const val CODE_IS_REQUIRED = "A unit code must have a name"
        const val UNIT_GROUP_ID_IS_REQUIRED = "A unit value must have a unit group id"
        const val CONVERSION_FACTOR_IS_REQUIRED = "A unit value with a base unit must have a conversion factor"
        const val BASE_UNIT_IS_REQUIRED = "A unit value with a conversion factor must have a base unit"
        const val PROVIDED_MISSING_UNIT_GROUP = "UnitGroup with the provided id does not exist"
        const val NAME_ALREADY_EXISTS = "A unit value with the name %s already exists"
        const val CODE_ALREADY_EXISTS = "A unit value with the code %s already exists"
        const val BASE_UNIT_MUST_BE_IN_GROUP = "The base unit must be selected from the assigned group"
    }
}
