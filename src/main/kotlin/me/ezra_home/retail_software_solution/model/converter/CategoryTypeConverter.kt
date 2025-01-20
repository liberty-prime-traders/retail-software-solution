package me.ezra_home.retail_software_solution.model.converter

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.model.enums.CategoryType

@Converter(autoApply = true)
class CategoryTypeConverter: EnumConverter<CategoryType>(CategoryType::class.java)