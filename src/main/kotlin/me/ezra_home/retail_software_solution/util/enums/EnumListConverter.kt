package me.ezra_home.retail_software_solution.util.enums

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.EnumSet

@Converter
open class EnumListConverter<ENUM>(private val enumClass: Class<ENUM>) : AttributeConverter<List<ENUM>, String?>
        where ENUM : Enum<ENUM>, ENUM : HasCode {

    override fun convertToDatabaseColumn(values: List<ENUM>?): String? =
        values?.joinToString(",") { it.code }

    override fun convertToEntityAttribute(value: String?): List<ENUM>? {
        if (value.isNullOrBlank()) return null
        val all = EnumSet.allOf(enumClass)
        return value.split(",").map { code -> all.first { it.code == code } }
    }
}
