package me.ezra_home.retail_software_solution.organizations.business.expense_type

import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumSetConverter

@Converter(autoApply = true)
class ExpensePayeeTypeSetConverter : EnumSetConverter<ContactType>(ContactType::class.java)
