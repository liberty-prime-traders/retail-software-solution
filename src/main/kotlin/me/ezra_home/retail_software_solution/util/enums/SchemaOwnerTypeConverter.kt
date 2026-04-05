package me.ezra_home.retail_software_solution.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
internal class SchemaOwnerTypeConverter : EnumConverter<SchemaOwnerType>(SchemaOwnerType::class.java)
