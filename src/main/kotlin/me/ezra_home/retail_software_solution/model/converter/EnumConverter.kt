package me.ezra_home.retail_software_solution.model.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.model.util.HasCode
import java.util.EnumSet

@Converter(autoApply = true)
open class EnumConverter<ENUM>(private val enumClass: Class<ENUM>) : AttributeConverter<ENUM, String?>
        where ENUM: Enum<ENUM>, ENUM: HasCode {

    override fun convertToDatabaseColumn(enumValue: ENUM?): String? {
        return enumValue?.code
    }

    override fun convertToEntityAttribute(code: String?): ENUM? {
        return EnumSet.allOf(enumClass).firstOrNull { enumValue -> enumValue.code.contentEquals(code) }
    }
}
