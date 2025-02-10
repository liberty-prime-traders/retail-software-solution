package me.ezra_home.retail_software_solution.business.unit

import me.ezra_home.retail_software_solution.business.unit.dto.UnitInsertDto
import me.ezra_home.retail_software_solution.business.unit.dto.UnitResponseDto
import me.ezra_home.retail_software_solution.business.unit.dto.UnitUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.model.entity.UnitEntity
import me.ezra_home.retail_software_solution.model.enums.DataType

object UnitValidator {
    fun validateInsert(unitInsertDto: UnitInsertDto, existingUnits: Collection<UnitEntity>) {
        val name = unitInsertDto.name?.takeIf { it.isNotBlank() }
            ?: throw RtsGenericException(UnitValidationMessages.NAME_IS_REQUIRED)

        val code = unitInsertDto.code?.takeIf { it.isNotBlank() }
            ?: throw RtsGenericException(UnitValidationMessages.CODE_IS_REQUIRED)

        val dataType = unitInsertDto.dataType?.takeIf { it.isNotBlank() }
            ?: throw RtsGenericException(UnitValidationMessages.DATA_TYPE_IS_REQUIRED)

        if (dataType !in listOf("TEXT", "NUMERIC")) {
            throw RtsGenericException(UnitValidationMessages.INVALID_DATA_TYPE)
        }

        if (dataType == "TEXT" && unitInsertDto.decimalCount != null) {
            throw RtsGenericException(UnitValidationMessages.DATA_TYPE_TEXT_WITH_DECIMAL_COUNT)
        }

        if (dataType == "NUMERIC" && unitInsertDto.decimalCount == null) {
            throw RtsGenericException(UnitValidationMessages.DATA_TYPE_NUMERIC_NULL_DECIMAL_COUNT)
        }

        if (existingUnits.any { it.name.equals(name, ignoreCase = true) }) {
            throw RtsGenericException(String.format(UnitValidationMessages.NAME_ALREADY_EXISTS, name))
        }

        if (existingUnits.any { it.code.equals(code, ignoreCase = true) }) {
            throw RtsGenericException(String.format(UnitValidationMessages.CODE_ALREADY_EXISTS, code))
        }

        if (unitInsertDto.enumerated == false && unitInsertDto.enumerationOptions != null) {
            throw RtsGenericException(UnitValidationMessages.ENUMERATION_OPTIONS_CANNOT_BE_MODIFIED)
        }

        if (unitInsertDto.enumerationOptions != null && unitInsertDto.enumerated != true) {
            throw RtsGenericException(UnitValidationMessages.ENUMERATION_OPTIONS_REQUIRE_ENUMERATED)
        }
    }

    fun validateUpdate(unitUpdateDto: UnitUpdateDto, existingUnit: UnitEntity, existingUnits: Collection<UnitEntity>) {
        val name = unitUpdateDto.name?.orElse(null)
            ?: throw RtsGenericException(UnitValidationMessages.NAME_IS_REQUIRED)

        if (existingUnits.any { it.name.equals(name, ignoreCase = true) && it.id != unitUpdateDto.id }) {
            throw RtsGenericException(String.format(UnitValidationMessages.NAME_ALREADY_EXISTS, name))
        }

        if (unitUpdateDto.dataType?.orElse(null) != null && unitUpdateDto.dataType.orElse(null) !in setOf("TEXT", "NUMERIC")) {
            throw RtsGenericException(UnitValidationMessages.INVALID_DATA_TYPE_UPDATE)
        }

        if ((unitUpdateDto.decimalCount?.isPresent == true) && (unitUpdateDto.dataType?.orElse(null) == "TEXT" || existingUnit.dataType == DataType.TEXT)){
            throw RtsGenericException(UnitValidationMessages.DECIMAL_COUNT_MODIFICATION_NOT_ALLOWED)
        }

        if (unitUpdateDto.enumerationOptions != null && unitUpdateDto.enumerated?.orElse(null) != true && existingUnit.enumerated != true) {
            throw RtsGenericException(UnitValidationMessages.ENUMERATION_OPTIONS_REQUIRE_ENUMERATED)
        }
    }
}

object UnitValidationMessages {
    const val NAME_IS_REQUIRED = "A unit must have a name"
    const val CODE_IS_REQUIRED = "A unit must have a code"
    const val DATA_TYPE_IS_REQUIRED = "A unit must have a data type"
    const val INVALID_DATA_TYPE = "Invalid data type. Allowed values are TEXT, NUMERIC"
    const val NAME_ALREADY_EXISTS = "A unit with the name %s is already assigned"
    const val CODE_ALREADY_EXISTS = "A unit with the code %s is already assigned"
    const val DATA_TYPE_TEXT_WITH_DECIMAL_COUNT = "A data type of TEXT should not have a decimal count"
    const val DATA_TYPE_NUMERIC_NULL_DECIMAL_COUNT = "A data type of NUMERIC should not have a null decimal count"
    const val ENUMERATION_OPTIONS_CANNOT_BE_MODIFIED = "Cannot modify enumeration_options when enumerated is false"
    const val ENUMERATION_OPTIONS_REQUIRE_ENUMERATED = "Enumeration options can only be set when enumerated is true"
    const val INVALID_DATA_TYPE_UPDATE = "Invalid data type update. Allowed values are TEXT, NUMERIC"
    const val DECIMAL_COUNT_MODIFICATION_NOT_ALLOWED = "Decimal count can only be modified when data_type is NUMERIC"
}
