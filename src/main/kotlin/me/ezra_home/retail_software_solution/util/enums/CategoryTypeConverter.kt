package me.ezra_home.retail_software_solution.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class CategoryTypeConverter: EnumConverter<CategoryType>(CategoryType::class.java)
