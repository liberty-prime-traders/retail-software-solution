package me.ezra_home.retail_software_solution.organizations.business.tag

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.organizations.business.tag.public.CategoryType
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class CategoryTypeConverter : EnumConverter<CategoryType>(CategoryType::class.java)
