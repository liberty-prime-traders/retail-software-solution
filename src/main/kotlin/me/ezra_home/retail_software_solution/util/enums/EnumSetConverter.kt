package me.ezra_home.retail_software_solution.util.enums

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.EnumSet

@Converter
open class EnumSetConverter<ENUM>(private val enumClass: Class<ENUM>) : AttributeConverter<Set<ENUM>, String?>
        where ENUM : Enum<ENUM>, ENUM : HasCode {

    override fun convertToDatabaseColumn(values: Set<ENUM>?): String? =
        values?.joinToString(",") { it.code }

    override fun convertToEntityAttribute(value: String?): Set<ENUM>? {
        if (value.isNullOrBlank()) return null
        val all = EnumSet.allOf(enumClass)
        return value.split(",").mapTo(LinkedHashSet()) { code -> all.first { it.code == code } }
    }
}
