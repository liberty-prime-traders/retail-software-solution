package me.ezra_home.retail_software_solution.organizations.business.expense_type

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.util.enums.EnumSetConverter

@Converter(autoApply = true)
class ExpenseSourceTypeSetConverter : EnumSetConverter<ExpenseSourceType>(ExpenseSourceType::class.java)
