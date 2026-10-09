package me.ezra_home.retail_software_solution.cross_tier.expense.entities

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter
class ExpenseSourceTypeConverter : EnumConverter<ExpenseSourceType>(ExpenseSourceType::class.java)
