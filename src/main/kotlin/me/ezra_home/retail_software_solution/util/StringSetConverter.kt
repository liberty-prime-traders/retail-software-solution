package me.ezra_home.retail_software_solution.util

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringSetConverter : AttributeConverter<Set<String>, String?> {

    override fun convertToDatabaseColumn(values: Set<String>?): String? =
        values?.takeIf { it.isNotEmpty() }?.joinToString(",")

    override fun convertToEntityAttribute(value: String?): Set<String> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(",")
            .filter { it.isNotBlank() }
            .toCollection(LinkedHashSet())
    }
}
